package api.m2.movements.controller;

import api.m2.movements.records.gamification.BadgeRecord;
import api.m2.movements.records.gamification.StreakRecord;
import api.m2.movements.services.gamification.BudgetBadgeService;
import api.m2.movements.services.gamification.StreakService;
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
@RequestMapping("/v1/gamification")
@Tag(name = "Gamification", description = "Rachas de registro y badges de presupuesto, del workspace activo")
public class GamificationController {

    private final StreakService streakService;
    private final BudgetBadgeService budgetBadgeService;

    @Operation(
            summary = "Racha de registro del usuario autenticado",
            description = "Días consecutivos registrando movimientos en el workspace activo. Si el usuario "
                    + "no registró nada ayer ni hoy, la racha actual se reporta rota (0), aunque el récord "
                    + "histórico se conserva.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Racha obtenida",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StreakRecord.class))
                    )
            }
    )
    @GetMapping("/streak")
    @ResponseStatus(HttpStatus.OK)
    public StreakRecord getStreak() {
        return streakService.getStreak();
    }

    @Operation(
            summary = "Badges obtenidos por el workspace activo",
            description = "Achievements de presupuesto cumplido, más reciente primero. Se otorgan al cierre "
                    + "de cada mes, no en tiempo real.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Listado de badges",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = BadgeRecord.class))
                            )
                    )
            }
    )
    @GetMapping("/badges")
    @ResponseStatus(HttpStatus.OK)
    public List<BadgeRecord> getBadges() {
        return budgetBadgeService.getBadges();
    }
}
