package api.m2.movements.controller;

import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.records.workspaces.WorkspaceInvitationDTO;
import api.m2.movements.records.workspaces.WorkspaceMemberDTO;
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
}