package api.expenses.expenses.services.groups;

import api.expenses.expenses.entities.GroupInvitation;
import api.expenses.expenses.entities.User;
import api.expenses.expenses.entities.UserGroups;
import api.expenses.expenses.enums.InvitationStatus;
import api.expenses.expenses.mappers.GroupInvitationMapperImpl;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import api.expenses.expenses.records.groups.InvitationResponseRecord;
import api.expenses.expenses.records.groups.UserRecord;
import api.expenses.expenses.repositories.GroupInvitationRepository;
import api.expenses.expenses.repositories.GroupRepository;
import api.expenses.expenses.repositories.UserRepository;
import api.expenses.expenses.services.user.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupInvitationAddServiceTest {
    @InjectMocks
    private GroupInvitationAddService service;

    @Mock
    private GroupInvitationRepository groupInvitationRepository;

    @Mock
    private UserService userService;

    @Mock
    private GroupRepository groupRepository;

    @Spy
    private GroupInvitationMapperImpl groupInvitationMapper;

    @Mock
    private UserRepository userRepository;

    private User loggedUser;
    private UserRecord loggedUserRecord;
    private UserGroups group;

    @BeforeEach
    void setUp() {
        loggedUser = User.builder().id(1L).email("logged@me.com").build();
        loggedUserRecord = new UserRecord(loggedUser.getEmail(), List.of(), loggedUser.getId());
        group = UserGroups.builder().id(10L).description("Test").build();
    }

    @Test
    @DisplayName("Devuelve las invitaciones pendientes del usuario")
    void getAllInvitations() {
        var record = new UserRecord("logged@me.com", null, 1L);
        var invitations = List.of(GroupInvitation.builder().id(5L).build());
        var mapped = List.of(new GroupInvitationRecord(5L, null));

        when(userService.getAuthenticatedUserRecord()).thenReturn(record);
        when(groupInvitationRepository.findAllByUserIdAndStatus(1L, InvitationStatus.PENDING))
                .thenReturn(invitations);
        when(groupInvitationMapper.toRecord(invitations)).thenReturn(mapped);

        var result = service.getAllInvitations();

        assertEquals(1, result.size());
        assertEquals(mapped, result);
    }

    @Test
    @DisplayName("Invita a nuevos usuarios al grupo")
    void inviteToGroupCreatesNewInvitations() {
        List<String> emails = List.of("a@test.com", "b@test.com");
        var userA = User.builder()
                .id(2L)
                .email("a@test.com")
                .userGroups(new HashSet<>())
                .build();
        var userB = User.builder()
                .id(3L)
                .email("b@test.com")
                .userGroups(new HashSet<>())
                .build();

        var savedInvitations = List.of(
                GroupInvitation.builder().id(100L).user(userA).group(group).build()
        );
        var mapped = List.of(new GroupInvitationRecord(100L, null));

        when(userService.getAuthenticatedUser()).thenReturn(loggedUser);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(userService.getUserByEmail(emails)).thenReturn(List.of(userA, userB));
        when(groupInvitationRepository.findAllByGroupIdAndStatus(10L, InvitationStatus.PENDING))
                .thenReturn(List.of()); // Nadie invitado aún
        when(groupInvitationRepository.saveAll(any())).thenReturn(savedInvitations);
        when(groupInvitationMapper.toRecord(savedInvitations)).thenReturn(mapped);

        var result = service.inviteToGroup(10L, emails);

        assertEquals(1, result.size());
        verify(groupInvitationRepository).saveAll(any());
    }

    @Test
    @DisplayName("No crea invitaciones si ya existen pendientes")
    void inviteToGroupAlreadyInvited() {
        List<String> emails = List.of("a@test.com");
        var userA = User.builder()
                .id(2L)
                .email("a@test.com")
                .build();

        var alreadyPending = GroupInvitation.builder()
                .id(50L)
                .user(userA)
                .status(InvitationStatus.PENDING)
                .build();
        var mapped = List.of(new GroupInvitationRecord(50L, null));

        when(userService.getAuthenticatedUser()).thenReturn(loggedUser);
        when(userService.getAuthenticatedUserRecord()).thenReturn(loggedUserRecord);
        when(groupInvitationRepository.findAllByUserIdAndStatus(loggedUserRecord.id(), InvitationStatus.PENDING))
                .thenReturn(List.of(alreadyPending));

        when(groupRepository.findById(10L))
                .thenReturn(Optional.of(group));

        when(userService.getUserByEmail(emails))
                .thenReturn(List.of(userA));

        when(groupInvitationRepository.findAllByGroupIdAndStatus(10L, InvitationStatus.PENDING))
                .thenReturn(List.of(alreadyPending));

        when(groupInvitationMapper.toRecord(List.of(alreadyPending))).thenReturn(mapped);

        var result = service.inviteToGroup(10L, emails);

        assertEquals(mapped, result);
        verify(groupInvitationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Lanza excepción si el grupo no existe")
    void inviteToGroupGroupNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> service.inviteToGroup(99L, List.of("foo@test.com")));

        verify(groupInvitationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Acepta invitación correctamente")
    void acceptInvitation() {
        var userToAdd = User.builder()
                .id(2L)
                .email("a@test.com")
                .userGroups(new HashSet<>())
                .build();

        var invitation = GroupInvitation.builder()
                .id(1L).status(InvitationStatus.PENDING).user(userToAdd).group(group).build();

        var mapped = List.of(new GroupInvitationRecord(1L, null));

        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(groupInvitationMapper.toRecord(any())).thenReturn(mapped);
        when(userService.getAuthenticatedUserRecord()).thenReturn(new UserRecord("a@test.com", null, 2L));

        var result = service.acceptRejectInvitation(1L, new InvitationResponseRecord(1L, true));

        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(userRepository).save(userToAdd);
        assertEquals(mapped, result);
    }

    @Test
    @DisplayName("Rechaza invitación correctamente sin agregar al grupo")
    void rejectInvitation() {
        var invitation = GroupInvitation.builder()
                .id(1L)
                .status(InvitationStatus.PENDING)
                .user(loggedUser)
                .group(group)
                .build();

        when(userService.getAuthenticatedUserRecord()).thenReturn(loggedUserRecord);
        when(groupInvitationRepository.findAllByUserIdAndStatus(loggedUserRecord.id(), InvitationStatus.PENDING))
                .thenReturn(List.of());

        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(groupInvitationMapper.toRecord(any())).thenReturn(List.of());

        var result = service.acceptRejectInvitation(1L, new InvitationResponseRecord(1L, false));

        assertEquals(InvitationStatus.REJECTED, invitation.getStatus());
        verify(userRepository, never()).save(any());
        assertNotNull(result);
    }

    @Test
    @DisplayName("No actualiza si la invitación no está en estado pendiente")
    void invitationWrongStatus() {
        var invitation = GroupInvitation.builder()
                .id(1L).status(InvitationStatus.ACCEPTED).build();

        when(userService.getAuthenticatedUserRecord()).thenReturn(loggedUserRecord);
        when(groupInvitationRepository.findAllByUserIdAndStatus(loggedUserRecord.id(), InvitationStatus.PENDING))
                .thenReturn(List.of());

        when(groupInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));
        when(groupInvitationMapper.toRecord(any())).thenReturn(List.of());

        var result = service.acceptRejectInvitation(1L, new InvitationResponseRecord(1L, true));

        verify(groupInvitationRepository, never()).save(any());
        assertNotNull(result);
    }
}