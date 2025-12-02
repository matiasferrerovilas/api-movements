package api.expenses.expenses.records.balance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "BalanceFilter",
        description = "Filtros para obtener el balance por fecha, grupos y monedas"
)
public record BalanceFilterRecord(
        @Schema(
                description = "Año del balance (yyyy)",
                example = "2025",
                minimum = "2000",
                maximum = "2100")
        Integer year,
        @Schema(
              description = "Mes del balance (1-12)",
              example = "12",
              minimum = "1",
              maximum = "12"
      )
      Integer month,

      @Schema(
              description = "Lista de grupos de gastos o ingresos a incluir",
              example = "[\"Comida\", \"Servicios\"]"
      )
      List<Integer> groups,

      @Schema(
              description = "Lista de monedas a incluir en el balance",
              example = "[\"USD\", \"ARS\"]"
      )
      List<String> currencies) {
}
