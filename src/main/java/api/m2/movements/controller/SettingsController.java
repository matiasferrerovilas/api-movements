package api.m2.movements.controller;

import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.LastIngresoRecord;
import api.m2.movements.services.movements.MovementGetService;
import api.m2.movements.services.settings.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/settings")
@Tag(name = "Settings", description = "API para la configuración")
public class SettingsController {
    private final SettingService settingService;
    private final MovementGetService movementGetService;

    @Operation(
            summary = "Registrar ingreso",
            description = "Crea un ingreso mensual"
    )
    @ApiResponse(responseCode = "201", description = "Ingreso registrado")
    @PostMapping
    public void addIngreso(@RequestBody @Valid IncomeToAdd incomeToAdd) {
        settingService.addIngreso(incomeToAdd);
    }

    @Operation(
            summary = "Último ingreso registrado",
            description = "Retorna el ingreso más reciente"
    )
    @ApiResponse(responseCode = "200", description = "Ingreso encontrado")
    @GetMapping("/last-ingreso")
    public LastIngresoRecord getLastIngreso() {
        return movementGetService.getLastIngreso();
    }
}