package api.m2.movements.controller;

import api.m2.movements.records.onboarding.OnBoardingForm;
import api.m2.movements.services.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/onboarding")
@Tag(name = "On Boarding", description = "API para el onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void finishOnboarding(@Valid @RequestBody OnBoardingForm onBoardingForm) {
        onboardingService.finish(onBoardingForm);
    }
}
