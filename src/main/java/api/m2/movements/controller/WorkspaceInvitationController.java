package api.m2.movements.controller;

import api.m2.movements.records.invite.InvitationResponseRecord;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.records.invite.InviteToWorkspace;
import api.m2.movements.services.invitations.InvitationAddService;
import api.m2.movements.services.invitations.InvitationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Workspace Invitations", description = "API para la gestión de invitaciones a workspaces")
public class WorkspaceInvitationController {

    private final InvitationAddService invitationAddService;
    private final InvitationQueryService invitationQueryService;

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
            @Valid @RequestBody InviteToWorkspace request
    ) {
        invitationAddService.inviteToWorkspace(id, request.emails());
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
        return invitationQueryService.getAllInvitations();
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
            @Valid @RequestBody InvitationResponseRecord body
    ) {
        invitationAddService.acceptRejectInvitation(invitationId, body);
    }
}