package api.expenses.expenses.controller;

import api.expenses.expenses.entities.Movement;
import api.expenses.expenses.records.groups.AddGroupRecord;
import api.expenses.expenses.records.groups.GroupInvitationRecord;
import api.expenses.expenses.records.groups.GroupsWIthUser;
import api.expenses.expenses.records.groups.InvitationResponseRecord;
import api.expenses.expenses.records.groups.InviteToGroup;
import api.expenses.expenses.records.groups.UserGroupsRecord;
import api.expenses.expenses.services.groups.GroupAddService;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.groups.GroupInvitationAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

import java.nio.file.AccessDeniedException;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/groups")
@Tag(name = "Grupos", description = "API para la gestión del grupos")

public class GroupController {
    private final GroupAddService groupAddService;
    private final GroupInvitationAddService groupInvitationAddService;
    private final GroupGetService groupGetService;

    @Operation(
            summary = "Crear un nuevo grupo",
            description = "Crea un grupo asociado al usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Grupo creado correctamente")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<GroupsWIthUser> createGroup(@RequestBody AddGroupRecord body) {
        return groupAddService.saveGroup(body);
    }

    @Operation(
            summary = "Listar grupos del usuario",
            description = "Devuelve todos los grupos a los que pertenece el usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Grupos obtenidos correctamente")
            }
    )
    @GetMapping("/count")
    public List<GroupsWIthUser> getMyGroupsWithCount() {
        return groupGetService.getMyGroupsWithCount();
    }

    @Operation(
            summary = "Obtener gastos",
            description = "Recupera una lista de gastos filtrados por diferentes criterios",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de gastos encontrados",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = Movement.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<UserGroupsRecord> getMyGroups() {
        return groupGetService.getMyGroups();
    }

    @Operation(
            summary = "Crear invitación a un grupo",
            description = "Invita a un usuario a un grupo mediante su email.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Invitación creada correctamente")
            }
    )
    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public void createInvitation(
            @PathVariable Long groupId,
            @RequestBody InviteToGroup request
    ) {
        groupInvitationAddService.inviteToGroup(groupId, request.emails());
    }

    @Operation(
            summary = "Listar invitaciones recibidas",
            description = "Devuelve todas las invitaciones con estado PENDING del usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invitaciones obtenidas correctamente")
            }
    )
    @GetMapping("/invitations")
    public List<GroupInvitationRecord> listMyInvitations() {
        return groupInvitationAddService.getAllInvitations();
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
    public List<GroupInvitationRecord> updateInvitationStatus(
            @PathVariable Long invitationId,
            @RequestBody InvitationResponseRecord body
    ) {
        return groupInvitationAddService.acceptRejectInvitation(invitationId, body);
    }

    @Operation(
            summary = "Salir de un grupo",
            description = "El usuario autenticado abandona un grupo.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Salida del grupo exitosa")
            }
    )
    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exitGroup(@PathVariable Long groupId) throws AccessDeniedException {
        groupAddService.exitGroup(groupId);
    }
}