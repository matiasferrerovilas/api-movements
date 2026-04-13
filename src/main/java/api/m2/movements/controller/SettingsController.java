package api.m2.movements.controller;

import api.m2.movements.enums.UserSettingKey;
import api.m2.movements.records.income.IncomeToAdd;
import api.m2.movements.records.settings.UserSettingRequest;
import api.m2.movements.records.settings.UserSettingResponse;
import api.m2.movements.services.income.IncomeAddService;
import api.m2.movements.services.settings.UserSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/settings")
@Tag(name = "Settings", description = "API para la configuración")
public class SettingsController {
    private final IncomeAddService incomeAddService;
    private final UserSettingService userSettingService;

    @Operation(
            summary = "Registrar ingreso",
            description = "Crea un ingreso mensual"
    )
    @ApiResponse(responseCode = "201", description = "Ingreso registrado")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addIngreso(@RequestBody @Valid IncomeToAdd incomeToAdd) {
        incomeAddService.addIngreso(incomeToAdd);
    }

    @Operation(
            summary = "Obtener todos los defaults del usuario",
            description = "Retorna todos los settings de default configurados para el usuario autenticado"
    )
    @ApiResponse(responseCode = "200", description = "Defaults del usuario")
    @GetMapping("/defaults")
    public List<UserSettingResponse> getAllDefaults() {
        return userSettingService.getAll();
    }

    @Operation(
            summary = "Obtener default por clave",
            description = "Retorna el setting de default para una clave específica (DEFAULT_ACCOUNT, DEFAULT_CURRENCY, DEFAULT_BANK)"
    )
    @ApiResponse(responseCode = "200", description = "Default encontrado")
    @ApiResponse(responseCode = "404", description = "Default no configurado para esa clave")
    @GetMapping("/defaults/{key}")
    public UserSettingResponse getDefaultByKey(@PathVariable UserSettingKey key) {
        return userSettingService.getByKey(key);
    }

    @Operation(
            summary = "Setear o actualizar un default",
            description = "Crea o actualiza el default para una clave específica"
    )
    @ApiResponse(responseCode = "200", description = "Default actualizado")
    @PutMapping("/defaults/{key}")
    @ResponseStatus(HttpStatus.OK)
    public UserSettingResponse upsertDefault(@PathVariable UserSettingKey key,
                                             @RequestBody @Valid UserSettingRequest request) {
        return userSettingService.upsert(key, request.value());
    }
}
