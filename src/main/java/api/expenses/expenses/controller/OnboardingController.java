package api.expenses.expenses.controller;

import api.expenses.expenses.records.onboarding.OnBoardingForm;
import api.expenses.expenses.services.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/onboarding")
@Tag(name = "On Boarding", description = "API para el onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;
    @Operation(
            summary = "Verifica si es el primer inicio de sesión del usuario",
            description = "Retorna `true` si el usuario aún no completó el onboarding.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estado del onboarding",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Boolean.class)
                            )
                    )
            }
    )
    @GetMapping("is-first")
    public boolean isFirstLogin() {
        return onboardingService.isFirstLogin();
    }

    @Operation(
            summary = "Completar el onboarding",
            description = "Marca el onboarding del usuario como finalizado.",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Onboarding completado correctamente",
                            content = @Content(schema = @Schema(hidden = true))
                    )
            }
    )
    @PostMapping
    public void finishOnboarding(@RequestBody OnBoardingForm onBoardingForm) {
        onboardingService.finish(onBoardingForm);
    }
}
