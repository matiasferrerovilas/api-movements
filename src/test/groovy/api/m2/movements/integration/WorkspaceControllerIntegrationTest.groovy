package api.m2.movements.integration

import api.m2.movements.entities.User
import api.m2.movements.entities.Workspace
import api.m2.movements.entities.WorkspaceInvitation
import api.m2.movements.entities.WorkspaceMember
import api.m2.movements.enums.InvitationStatus
import api.m2.movements.enums.WorkspaceRole
import api.m2.movements.repositories.WorkspaceInvitationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class WorkspaceControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    WorkspaceInvitationRepository invitationRepository

    def "POST /v1/workspace - should create workspace and return 201"() {
        given:
        def request = [description: "Mi nuevo workspace"]

        when:
        def result = mockMvc.perform(post("/v1/workspace")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())

        and:
        def workspaces = workspaceRepository.findAll()
        workspaces.any { it.name == "Mi nuevo workspace" }
    }

    def "POST /v1/workspace - should return 400 for blank description"() {
        given:
        def request = [description: ""]

        when:
        def result = mockMvc.perform(post("/v1/workspace")
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "GET /v1/workspace/membership - should return user memberships"() {
        when:
        def result = mockMvc.perform(get("/v1/workspace/membership")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$.length()').value(1))
    }

    def "GET /v1/workspace/count - should return workspaces with member count"() {
        when:
        def result = mockMvc.perform(get("/v1/workspace/count")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
    }

    def "DELETE /v1/workspace/{workspaceId} - should allow member to leave workspace"() {
        given:
        def anotherUser = userRepository.save(User.builder()
                .email("owner@test.com")
                .isFirstLogin(false)
                .build())

        def workspace = workspaceRepository.save(Workspace.builder()
                .name("Workspace to leave")
                .owner(anotherUser)
                .build())

        membershipRepository.save(WorkspaceMember.builder()
                .user(anotherUser)
                .workspace(workspace)
                .role(WorkspaceRole.OWNER)
                .build())

        membershipRepository.save(WorkspaceMember.builder()
                .user(testUser)
                .workspace(workspace)
                .role(WorkspaceRole.COLLABORATOR)
                .build())

        when:
        def result = mockMvc.perform(delete("/v1/workspace/{workspaceId}", workspace.id)
                .with(jwtAuth()))

        then:
        result.andExpect(status().isNoContent())
    }

    def "POST /v1/workspace/{id}/invitations - should create invitation"() {
        given:
        def inviteeEmail = "invitee@test.com"
        def request = [emails: [inviteeEmail]]

        userRepository.save(User.builder()
                .email(inviteeEmail)
                .isFirstLogin(false)
                .build())

        when:
        def result = mockMvc.perform(post("/v1/workspace/{id}/invitations", testWorkspace.id)
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())
    }

    def "GET /v1/workspace/invitations - should return pending invitations"() {
        given:
        def inviter = userRepository.save(User.builder()
                .email("inviter@test.com")
                .isFirstLogin(false)
                .build())

        def otherWorkspace = workspaceRepository.save(Workspace.builder()
                .name("Other Workspace")
                .owner(inviter)
                .build())

        invitationRepository.save(WorkspaceInvitation.builder()
                .user(testUser)
                .workspace(otherWorkspace)
                .invitedBy(inviter)
                .status(InvitationStatus.PENDING)
                .build())

        when:
        def result = mockMvc.perform(get("/v1/workspace/invitations")
                .with(jwtAuth()))

        then:
        result.andExpect(status().isOk())
                .andExpect(jsonPath('$').isArray())
                .andExpect(jsonPath('$.length()').value(1))
    }

    def "PATCH /v1/workspace/invitations/{invitationId} - should accept invitation"() {
        given:
        def inviter = userRepository.save(User.builder()
                .email("inviter2@test.com")
                .isFirstLogin(false)
                .build())

        def otherWorkspace = workspaceRepository.save(Workspace.builder()
                .name("Workspace to join")
                .owner(inviter)
                .build())

        membershipRepository.save(WorkspaceMember.builder()
                .user(inviter)
                .workspace(otherWorkspace)
                .role(WorkspaceRole.OWNER)
                .build())

        def invitation = invitationRepository.save(WorkspaceInvitation.builder()
                .user(testUser)
                .workspace(otherWorkspace)
                .invitedBy(inviter)
                .status(InvitationStatus.PENDING)
                .build())

        def request = [id: invitation.id, status: true]

        when:
        def result = mockMvc.perform(patch("/v1/workspace/invitations/{invitationId}", invitation.id)
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isOk())

        and:
        def updated = invitationRepository.findById(invitation.id).get()
        updated.status == InvitationStatus.ACCEPTED
    }

    def "PATCH /v1/workspace/invitations/{invitationId} - should reject invitation"() {
        given:
        def inviter = userRepository.save(User.builder()
                .email("inviter3@test.com")
                .isFirstLogin(false)
                .build())

        def otherWorkspace = workspaceRepository.save(Workspace.builder()
                .name("Workspace to reject")
                .owner(inviter)
                .build())

        def invitation = invitationRepository.save(WorkspaceInvitation.builder()
                .user(testUser)
                .workspace(otherWorkspace)
                .invitedBy(inviter)
                .status(InvitationStatus.PENDING)
                .build())

        def request = [id: invitation.id, status: false]

        when:
        def result = mockMvc.perform(patch("/v1/workspace/invitations/{invitationId}", invitation.id)
                .with(jwtAuth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isOk())

        and:
        def updated = invitationRepository.findById(invitation.id).get()
        updated.status == InvitationStatus.REJECTED
    }

    def "GET /v1/workspace/membership - should require authentication"() {
        when:
        def result = mockMvc.perform(get("/v1/workspace/membership"))

        then:
        result.andExpect(status().isUnauthorized())
    }
}
