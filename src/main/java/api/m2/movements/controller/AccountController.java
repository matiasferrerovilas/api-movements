package api.m2.movements.controller;

import api.m2.movements.records.accounts.AccountInvitationRecord;
import api.m2.movements.records.accounts.AccountRecord;
import api.m2.movements.records.accounts.AccountsWithUser;
import api.m2.movements.records.groups.AddGroupRecord;
import api.m2.movements.records.groups.InvitationResponseRecord;
import api.m2.movements.records.groups.InviteToGroup;
import api.m2.movements.services.accounts.AccountAddService;
import api.m2.movements.services.accounts.AccountQueryService;
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
@RequestMapping("/v1/account")
@Tag(name = "Grupos de cuenta", description = "API para la gestión del grupos")
public class AccountController {

    private final AccountAddService accountAddService;
    private final AccountQueryService accountQueryService;
    private final InvitationService invitationService;
    @Operation(
            summary = "Crear un nuevo grupo",
            description = "Crea un grupo asociado al usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Grupo creado correctamente")
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createAccount(@RequestBody AddGroupRecord body) {
        accountAddService.createAccount(body);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<AccountRecord> getAllAccounts() {
        return accountQueryService.findAllAccountsOfLogInUser();
    }

    @Operation(
            summary = "Listar grupos del usuario",
            description = "Devuelve todos los grupos a los que pertenece el usuario autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Grupos obtenidos correctamente")
            }
    )
    @GetMapping("/count")
    public List<AccountsWithUser> getMyGroupsWithCount() {
        return accountQueryService.getAllAccountsWithUserCount();
    }

    @Operation(
            summary = "Salir de un grupo",
            description = "El usuario autenticado abandona un grupo.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Salida del grupo exitosa")
            }
    )
    @DeleteMapping("/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void exitGroup(@PathVariable Long accountId) {
        accountAddService.leaveAccount(accountId);
    }

    @Operation(
            summary = "Crear invitación a un grupo",
            description = "Invita a un usuario a un grupo mediante su email.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Invitación creada correctamente")
            }
    )
    @PostMapping("/{accountId}/invitations")
    @ResponseStatus(HttpStatus.OK)
    public void createInvitation(
            @PathVariable Long accountId,
            @RequestBody InviteToGroup request
    ) {
        invitationService.inviteToAccount(accountId, request.emails());
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
    public List<AccountInvitationRecord> listMyInvitations() {
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
}
