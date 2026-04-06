package api.m2.movements.unit.services

import api.m2.movements.entities.Account
import api.m2.movements.entities.AccountInvitation
import api.m2.movements.entities.User
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.exceptions.PermissionDeniedException
import api.m2.movements.mappers.AccountInvitationMapper
import api.m2.movements.records.invite.InvitationResponseRecord
import api.m2.movements.repositories.AccountInvitationRepository
import api.m2.movements.services.groups.AccountQueryService
import api.m2.movements.services.groups.GroupAddService
import api.m2.movements.services.invitations.InvitationService
import api.m2.movements.services.publishing.websockets.AccountPublishServiceWebSocket
import api.m2.movements.services.user.UserService
import org.mapstruct.factory.Mappers
import spock.lang.Specification

class InvitationServiceTest extends Specification {

    GroupAddService groupAddService = Mock()
    UserService userService = Mock()
    AccountInvitationRepository accountInvitationRepository = Mock()
    AccountPublishServiceWebSocket accountPublishServiceWebSocket = Mock()
    AccountQueryService accountQueryService = Mock()

    AccountInvitationMapper accountInvitationMapper

    InvitationService service

    def setup() {
        accountInvitationMapper = Mappers.getMapper(AccountInvitationMapper)

        service = new InvitationService(
                groupAddService,
                accountQueryService,
                userService,
                accountInvitationRepository,
                accountInvitationMapper,
                accountPublishServiceWebSocket
        )
    }

    // --- inviteToAccount ---

    def "inviteToAccount - should throw PermissionDeniedException when caller is not member of account"() {
        given:
        def caller = User.builder().id(10L).email("caller@test.com").build()
        def account = Account.builder().id(1L).name("Mi cuenta").owner(caller).build()

        accountQueryService.findAccountById(1L) >> account
        userService.getAuthenticatedUser() >> caller
        accountQueryService.verifyUserIsMemberOfAccount(1L, 10L) >> {
            throw new PermissionDeniedException("No tienes permiso para operar sobre este recurso")
        }

        when:
        service.inviteToAccount(1L, ["invited@test.com"])

        then:
        thrown(PermissionDeniedException)
        0 * accountInvitationRepository.saveAll(_ as List)
    }

    def "inviteToAccount - should create invitations when caller is member of account"() {
        given:
        def caller = User.builder().id(10L).email("caller@test.com").build()
        def account = Account.builder().id(1L).name("Mi cuenta").owner(caller).build()
        def invited = User.builder().id(20L).email("invited@test.com").build()

        accountQueryService.findAccountById(1L) >> account
        userService.getAuthenticatedUser() >> caller
        accountQueryService.verifyUserIsMemberOfAccount(1L, 10L) >> {}
        userService.getUserByEmail(["invited@test.com"]) >> [invited]
        accountInvitationRepository.findAllByAccountIdAndStatus(1L, InvitationStatus.PENDING) >> []
        accountInvitationRepository.saveAll(_ as List) >> []

        when:
        service.inviteToAccount(1L, ["invited@test.com"])

        then:
        1 * accountInvitationRepository.saveAll(_ as List)
        1 * accountPublishServiceWebSocket.publishInvitationAdded(_ as Object)
    }

    // --- acceptRejectInvitation ---

    def "acceptRejectInvitation - should throw PermissionDeniedException when invitation does not belong to current user"() {
        given:
        def owner = User.builder().id(99L).email("owner@test.com").build()
        def currentUser = User.builder().id(10L).email("other@test.com").build()
        def invitation = AccountInvitation.builder()
                .id(5L)
                .user(owner)
                .status(InvitationStatus.PENDING)
                .build()

        accountInvitationRepository.findById(5L) >> Optional.of(invitation)
        userService.getAuthenticatedUser() >> currentUser

        when:
        service.acceptRejectInvitation(5L, new InvitationResponseRecord(5L, true))

        then:
        thrown(PermissionDeniedException)
        0 * accountInvitationRepository.save(_ as AccountInvitation)
    }

    def "acceptRejectInvitation - should accept invitation when it belongs to current user"() {
        given:
        def currentUser = User.builder().id(10L).email("user@test.com").build()
        def account = Account.builder().id(1L).name("Mi cuenta").owner(currentUser).build()
        def invitation = AccountInvitation.builder()
                .id(5L)
                .user(currentUser)
                .account(account)
                .status(InvitationStatus.PENDING)
                .build()

        accountInvitationRepository.findById(5L) >> Optional.of(invitation)
        userService.getAuthenticatedUser() >> currentUser

        when:
        service.acceptRejectInvitation(5L, new InvitationResponseRecord(5L, true))

        then:
        1 * accountInvitationRepository.save(_ as AccountInvitation)
        1 * groupAddService.addMemberToAccount(account)
        1 * accountPublishServiceWebSocket.publishInvitationUpdated(_ as Object)
    }
}
