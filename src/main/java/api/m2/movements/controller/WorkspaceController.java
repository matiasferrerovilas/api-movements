package api.m2.movements.controller;

import api.m2.movements.projections.MembershipSummaryProjection;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.records.invite.InvitationResponseRecord;
import api.m2.movements.records.invite.InviteToWorkspace;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import api.m2.movements.services.groups.MembershipService;
import api.m2.movements.services.invitations.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspace")
@Tag(name = "Workspaces", description = "API para la gestión de workspaces")
public class WorkspaceController {

    private final WorkspaceAddService workspaceAddService;
    private final WorkspaceQueryService workspaceQueryService;
    private final InvitationService invitationService;
    private final MembershipService membershipService;

    @Operation(
            summary = "Crear un nuevo workspace",
            description = "Crea un workspace asociado al usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Workspace creado correctamente")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createWorkspace(@RequestBody AddWorkspaceRecord body) {
        workspaceAddService.createWorkspace(body);
    }

    @GetMapping("/membership")
    @ResponseStatus(HttpStatus.OK)
    public List<MembershipSummaryProjection> getAllMemberships() {
        return membershipService.getAllMemberships();
    }

    @Operation(
            summary = "Listar workspaces del usuario",
            description = "Devuelve todos los workspaces a los que pertenece el usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workspaces obtenidos correctamente")
            }
    )
    @GetMapping("/count")
    public List<WorkspaceDetail> getMyWorkspacesWithCount() {
        return workspaceQueryService.getAllWorkspaceDetails();
    }

    @Operation(
            summary = "Salir de un workspace",
            description = "El usuario autenticado abandona un workspace.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Salida del workspace exitosa")
            }
    )
    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exitWorkspace(@PathVariable Long workspaceId) {
        workspaceAddService.leaveWorkspace(workspaceId);
    }

    @Operation(
            summary = "Crear invitación a un workspace",
            description = "Invita a un usuario a un workspace mediante su email.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Invitación creada correctamente")
            }
    )
    @PostMapping("/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createInvitation(
            @PathVariable Long id,
            @RequestBody InviteToWorkspace request
    ) {
        invitationService.inviteToAccount(id, request.emails());
    }

    @Operation(
            summary = "Listar invitaciones recibidas",
            description = "Devuelve todas las invitaciones con estado PENDING del usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @GetMapping("/invitations")
    @ResponseStatus(HttpStatus.OK)
    public List<InvitationToWorkspaceRecord> listMyInvitations() {
        return invitationService.getAllInvitations();
    }

    @Operation(
            summary = "Actualizar estado de invitación",
            description = "Permite aceptar o rechazar una invitación.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitación actualizada correctamente"),
                    @ApiResponse(responseCode = "404", description = "Invitación no encontrada")
            }
    )
    @PatchMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.OK)
    public void updateInvitationStatus(
            @PathVariable Long invitationId,
            @RequestBody InvitationResponseRecord body
    ) {
        invitationService.acceptRejectInvitation(invitationId, body);
    }

    @Operation(
            summary = "Actualizar workspace por defecto",
            description = "Establece un workspace como el workspace por defecto del usuario.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workspace por defecto actualizado correctamente")
            }
    )
    @PatchMapping("/{id}/default")
    @ResponseStatus(HttpStatus.OK)
    public void updateDefaultWorkspace(@PathVariable Long id) {
        workspaceAddService.updateDefaultWorkspace(id);
    }
}
