package api.m2.movements.controller;

import api.m2.movements.projections.MembershipSummaryProjection;
import api.m2.movements.records.categories.CategoryMigrateRequest;
import api.m2.movements.records.categories.CategoryPatchRequest;
import api.m2.movements.records.categories.CategoryRecord;
import api.m2.movements.records.invite.InvitationResponseRecord;
import api.m2.movements.records.invite.InvitationToWorkspaceRecord;
import api.m2.movements.records.invite.InviteToWorkspace;
import api.m2.movements.records.workspaces.AddWorkspaceRecord;
import api.m2.movements.records.workspaces.WorkspaceDetail;
import api.m2.movements.services.category.CategoryMigrateService;
import api.m2.movements.services.category.WorkspaceCategoryService;
import api.m2.movements.services.groups.MembershipService;
import api.m2.movements.services.invitations.InvitationAddService;
import api.m2.movements.services.invitations.InvitationQueryService;
import api.m2.movements.services.workspaces.WorkspaceAddService;
import api.m2.movements.services.workspaces.WorkspaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final InvitationAddService invitationAddService;
    private final InvitationQueryService invitationQueryService;
    private final MembershipService membershipService;
    private final WorkspaceCategoryService workspaceCategoryService;
    private final CategoryMigrateService categoryMigrateService;

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
            summary = "Listar miembros del workspace activo",
            description = "Devuelve los emails de todos los miembros del workspace activo.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de emails de miembros",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = String.class))
                            )
                    )
            }
    )
    @GetMapping("/members")
    public List<String> getWorkspaceMembers() {
        return membershipService.getMemberEmails();
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

    // ==================== CATEGORIES ====================

    @Operation(
            summary = "Obtener categorias activas del workspace activo",
            description = "Recupera la lista de categorias activas asociadas al workspace activo del usuario",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de categorias del workspace",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(
                                            schema = @Schema(implementation = CategoryRecord.class)
                                    )
                            )
                    )
            }
    )
    @GetMapping("/categories")
    public List<CategoryRecord> getCategories() {
        return workspaceCategoryService.getActiveCategories();
    }

    @Operation(
            summary = "Agregar categoria al workspace activo",
            description = "Crea la categoria si no existe y la asocia al workspace activo. Es idempotente.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Categoria creada o reactivada")
            }
    )
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRecord addCategory(@RequestParam String description) {
        return workspaceCategoryService.addCategory(description);
    }

    @Operation(
            summary = "Actualizar categoría",
            description = "Actualiza la descripción, ícono y/o color de una categoría. "
                    + "Todos los campos son opcionales.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
                    @ApiResponse(responseCode = "404", description = "Categoría no encontrada")
            }
    )
    @PatchMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    public CategoryRecord updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPatchRequest request
    ) {
        return workspaceCategoryService.updateCategory(categoryId, request);
    }

    @Operation(
            summary = "Eliminar categoria del workspace activo",
            description = "Elimina la asociacion entre el workspace activo y la categoria. "
                    + "No elimina la categoria global.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Categoria eliminada"),
                    @ApiResponse(responseCode = "404", description = "Categoria no encontrada")
            }
    )
    @DeleteMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long categoryId) {
        workspaceCategoryService.deleteCategory(categoryId);
    }

    @Operation(
            summary = "Migrar movimientos de una categoria a otra",
            description = "Reasigna los movimientos del workspace activo de fromCategoryId a toCategoryId. "
                    + "Solo ADMIN.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Migracion completada"),
                    @ApiResponse(responseCode = "400",
                            description = "fromCategoryId igual a toCategoryId"),
                    @ApiResponse(responseCode = "404", description = "Categoria destino no encontrada")
            }
    )
    @PatchMapping("/categories/migrate")
    @PreAuthorize("hasRole('ADMIN')")
    public void migrateCategory(@Valid @RequestBody CategoryMigrateRequest request) {
        categoryMigrateService.migrateCategory(request);
    }
}
