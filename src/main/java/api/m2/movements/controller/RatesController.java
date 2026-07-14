package api.m2.movements.controller;

import api.m2.movements.records.currencies.ExchangeRateRecord;
import api.m2.movements.services.currencies.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/rates")
@Tag(name = "Cotizaciones", description = "Cotizaciones de moneda via Frankfurter")
public class RatesController {

    private final ExchangeRateService exchangeRateService;

    @Operation(
            summary = "Obtener cotizaciones",
            description = "Retorna cotizaciones desde Frankfurter para la moneda base y las monedas consultadas.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista de cotizaciones",
                            content = @Content(
                                    mediaType = "application/json",
                                    array = @ArraySchema(schema = @Schema(implementation = ExchangeRateRecord.class))
                            )
                    )
            }
    )
    @GetMapping
    public List<ExchangeRateRecord> getRates(
            @RequestParam String base,
            @RequestParam String quotes) {
        return exchangeRateService.getRates(base, quotes);
    }
}
