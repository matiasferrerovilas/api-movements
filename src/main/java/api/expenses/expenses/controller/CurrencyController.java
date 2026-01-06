package api.expenses.expenses.controller;

import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.services.currencies.CurrencyAddService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/currency")
@Tag(name = "Monedas", description = "API para la gestión de monedas soportadas")
public class CurrencyController {
  private final CurrencyAddService currencyAddService;

  @Operation(
      summary = "Obtener todas las monedas",
      description = "Recupera una lista de monedas",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Lista de monedas encontradas",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = CurrencyRecord.class))
              )
          )
      }
  )
  @GetMapping
  public List<CurrencyRecord> getAllCategories() {
    return currencyAddService.getAllCurrencies();
  }
}
