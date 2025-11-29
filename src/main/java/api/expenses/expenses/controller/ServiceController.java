package api.expenses.expenses.controller;

import api.expenses.expenses.records.services.ServiceRecord;
import api.expenses.expenses.records.services.ServiceToAdd;
import api.expenses.expenses.records.services.UpdateServiceRecord;
import api.expenses.expenses.services.services.UtilitiesService;
import api.expenses.expenses.services.services.UtilityAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/services")
@Tag(name = "Services", description = "API para la gestión de servicios personales")
public class ServiceController {
    private final UtilitiesService utilitiesService;
    private final UtilityAddService utilityAddService;

    @Operation(
            summary = "Obtener servicios",
            description = "Recupera una lista de servicios filtrados por diferentes criterios",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de servicios encontrados",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ServiceRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<ServiceRecord> getServiceBy(
            @Parameter(description = "ID de la moneda para filtrar")
            @RequestParam(required = false) List<String> currencySymbol,
            @Parameter(description = "Fecha de ultimo pago del servicio")
            @RequestParam(required = false) LocalDate lastPayment) {
        return utilitiesService.getServiceBy(currencySymbol, lastPayment);
    }

    @Operation(
            summary = "Crear servicios",
            description = "Creo un servicio",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Servicio creado",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ServiceRecord.class))
                            )
                    )
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRecord saveService(@RequestBody ServiceToAdd serviceToAdd) {
        return utilityAddService.save(serviceToAdd);
    }

    @Operation(
            summary = "Registrar pago del servicio",
            description = "Registra el pago de un servicio existente y genera un movimiento asociado",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Servicio pagado correctamente",
                            content = @Content(schema = @Schema(implementation = ServiceRecord.class))
                    )
            }
    )
    @PatchMapping("/{id}/payment")
    @ResponseStatus(HttpStatus.OK)
    public void payService(@PathVariable Long id) {
        utilitiesService.payServiceById(id);
    }

    @Operation(
            summary = "Actualizar un servicio",
            description = "Actualiza los datos de un servicio existente",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Servicio actualizado correctamente",
                            content = @Content(schema = @Schema(implementation = ServiceRecord.class))
                    )
            }
    )
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void updateService(@PathVariable Long id, @RequestBody UpdateServiceRecord updateService) {
        utilitiesService.updateService(id, updateService);
    }

    @Operation(
            summary = "Eliminar un servicio",
            description = "Elimina un servicio existente por ID",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Servicio eliminado correctamente"
                    )
            }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long id) {
        utilitiesService.deleteService(id);
    }
}