package api.expenses.expenses.controller;

import api.expenses.expenses.records.groups.GroupInvitationRecord;
import api.expenses.expenses.records.groups.InvitationResponseRecord;
import api.expenses.expenses.services.groups.GroupGetService;
import api.expenses.expenses.services.groups.GroupInvitationAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/groups")
@Tag(name = "Grupos", description = "API para la gestión del grupos")
public class GroupController {
    private final GroupInvitationAddService groupInvitationAddService;
    private final GroupGetService groupGetService;



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
}