package api.expenses.expenses.records.movements;

import api.expenses.expenses.enums.BanksEnum;
import api.expenses.expenses.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

public record MovementSearchFilterRecord(
        @Schema(description = "Métodos de pago para filtrar")
        List<MovementType> type,

        @Schema(description = "Símbolos de moneda para filtrar (por ejemplo: USD, EUR, ARS)")
        List<String> currency,

        @Schema(description = "Bancos asociados al gasto")
        List<BanksEnum> bank,

        @Schema(description = "Categorias asociados al gasto")
        List<String> categories,

        @Schema(description = "Descripcion asociados al gasto")
        String description,

        @Schema(description = "Fecha desde asociada al gasto")
        LocalDate dateFrom,

        @Schema(description = "Fecha hasta asociada al gasto")
        LocalDate dateTO,

        @Schema(description = "Booleano que indica si trae los movimientos actuales o no, si es true el dateFrom dateTo no aplica")
        Boolean isLive
) {}
