package api.m2.movements.records.balance;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(
        name = "BalanceFilter",
        description = "Filtros para obtener el balance por fecha y monedas"
)
@RegisterReflectionForBinding
public record BalanceFilterRecord(
      LocalDate startDate,
      LocalDate endDate,
      @Schema(
              description = "Lista de monedas a incluir en el balance",
              example = "[\"USD\", \"ARS\"]"
      )
      List<String> currencies) {
}
