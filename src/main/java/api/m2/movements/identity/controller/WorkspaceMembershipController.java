package api.m2.movements.identity.controller;

import api.m2.movements.identity.projections.MembershipSummaryProjection;
import api.m2.movements.identity.services.membership.MembershipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/workspace")
@Tag(name = "Workspace Membership", description = "API para la gestión de membresías de workspaces")
public class WorkspaceMembershipController {

    private final MembershipService membershipService;

    @GetMapping("/membership")
    @ResponseStatus(HttpStatus.OK)
    public List<MembershipSummaryProjection> getAllMemberships() {
        return membershipService.getAllMemberships();
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
}