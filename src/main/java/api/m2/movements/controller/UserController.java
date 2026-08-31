package api.m2.movements.controller;

import api.m2.movements.clients.identity.response.UserMe;
import api.m2.movements.services.settings.UserSettingService;
import api.m2.movements.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users", description = "API de usuarios")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    // Inyectado acá (no en UserService) a propósito: UserSettingService ya depende de UserService
    // para resolver el userId, así que inyectarlo al revés crearía un ciclo de beans. El
    // controller sí puede depender de los dos sin problema.
    private final UserSettingService userSettingService;

    @Operation(
            summary = "Obtener datos del usuario autenticado",
            description = "Retorna el ID interno, email, estado de onboarding y tipo de usuario del usuario autenticado. "
                    + "Si el usuario no existe aún en la base de datos, retorna isFirstLogin=true con los demás campos en null. "
                    + "Si el usuario tiene un workspace por defecto configurado, además incluye su rol en ese workspace.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Datos del usuario autenticado",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserMe.class)
                            )
                    )
            }
    )
    @GetMapping("/me")
    public UserMe getMe() {
        UserMe me = userService.getMe();
        return userSettingService.getDefaultWorkspaceId(me.id())
                .map(userService::getMe)
                .orElse(me);
    }
}
