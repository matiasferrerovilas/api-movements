package api.m2.movements.controller;

import api.m2.movements.clients.identity.requests.AcceptRejectInvitationDTO;
import api.m2.movements.clients.identity.requests.WorkspaceSendInvitationDTO;
import api.m2.movements.clients.identity.requests.AddWorkspaceRecord;
import api.m2.movements.clients.identity.response.WorkspaceInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceSentInvitationDTO;
import api.m2.movements.clients.identity.response.WorkspaceMemberDTO;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @Operation(
            summary = "Crear un nuevo workspace",
            description = "Crea un workspace asociado al usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Workspace creado correctamente")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createWorkspace(@Valid @RequestBody AddWorkspaceRecord body) {
        workspaceAddService.createWorkspace(body);
    }

    @Operation(
            summary = "Listar workspaces del usuario",
            description = "Pendiente de implementación.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Workspaces obtenidos correctamente")
            }
    )
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceMemberDTO> getWorkspaces() {
        return workspaceQueryService.getWorkspaces();
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
            summary = "Eliminar un miembro de un workspace",
            description = "Elimina al usuario indicado del workspace. Requiere ser OWNER del workspace o "
                    + "tener el rol global ROLE_ADMIN (verificado por api-identity).",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Miembro eliminado"),
                    @ApiResponse(responseCode = "403", description = "Quien invoca no es OWNER ni administrador")
            }
    )
    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long workspaceId, @PathVariable Long userId) {
        workspaceAddService.removeMember(workspaceId, userId);
    }

    @Operation(
            summary = "Listar invitaciones recibidas",
            description = "Devuelve todas las invitaciones pendientes del usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @GetMapping("/invitations")
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceInvitationDTO> getMyInvitations() {
        return workspaceQueryService.getMyInvitations();
    }

    @Operation(
            summary = "Listar invitaciones recibidas",
            description = "Devuelve todas las invitaciones pendientes del usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.OK)
    public void sendInvitation(@PathVariable Long workspaceId, @Valid @RequestBody WorkspaceSendInvitationDTO body) {
        workspaceQueryService.sendInvitation(workspaceId, body);
    }

    @Operation(
            summary = "Aceptar rechazar invitaciones",
            description = "Aceptar rechazar invitaciones a workspaces.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @PatchMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.OK)
    public void acceptRejectInvitation(@PathVariable Long invitationId, @RequestBody AcceptRejectInvitationDTO invitationDTO) {
        workspaceQueryService.acceptRejectInvitation(invitationDTO);
    }

    @Operation(
            summary = "Listar invitaciones enviadas",
            description = "Devuelve todas las invitaciones enviadas por el usuario autenticado, más reciente primero.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @GetMapping("/invitations/sent")
    @ResponseStatus(HttpStatus.OK)
    public List<WorkspaceSentInvitationDTO> getSentInvitations() {
        return workspaceQueryService.getSentInvitations();
    }

    @Operation(
            summary = "Cancelar una invitación enviada",
            description = "Cancela una invitación pendiente enviada por el usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitación cancelada")
            }
    )
    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.OK)
    public void cancelInvitation(@PathVariable Long invitationId) {
        workspaceQueryService.cancelInvitation(invitationId);
    }

}