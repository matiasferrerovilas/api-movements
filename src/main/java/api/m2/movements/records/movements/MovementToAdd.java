package api.m2.movements.records.movements;

import api.m2.movements.constraints.ValidCuotas;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ValidCuotas
public record MovementToAdd(
        @NotNull(message = "El monto es requerido")
        @Positive(message = "El monto debe ser mayor a cero")
        BigDecimal amount,
        @NotNull(message = "La fecha no puede ser nula")
        LocalDate date,
        @NotBlank(message = "Debe indicar una descripción")
        String description,
        List<CategoryUpdateRecord> categories,
        @NotBlank(message = "Debe indicar un tipo de gasto")
        String type,
        @NotBlank(message = "Debe indicar un tipo de moneda")
        String currency,
        Integer cuotaActual,
        Integer cuotasTotales,
        String bank,
        // Solo lo manda el cron de cuotas (CreditInstallmentJob), copiando el valor ya calculado
        // de la cuota anterior — cualquier otro caller lo deja en null y MovementFactory lo
        // calcula solo si type=CREDITO. Nunca lo completa el frontend.
        LocalDate lastCreditPayment,
        // Desglose opcional del movimiento (ej: "1kg papa", "2 tomates"). Null o vacío si el
        // usuario no lo usa — no se valida contra `amount`.
        List<MovementItemDto> items,
        // Clave de idempotencia opcional (UUID generado por el cliente). Si el cliente reenvía el
        // mismo alta (ej: la respuesta se perdió y reintenta desde su cola offline), el servidor
        // devuelve el movimiento ya creado en vez de duplicarlo.
        @Size(max = 36, message = "El clientId no puede superar 36 caracteres")
        String clientId
) {
    public MovementToAdd(BigDecimal amount, LocalDate date, String description,
                         List<CategoryUpdateRecord> categories, String type, String currency,
                         Integer cuotaActual, Integer cuotasTotales, String bank,
                         LocalDate lastCreditPayment, List<MovementItemDto> items) {
        this(amount, date, description, categories, type, currency, cuotaActual, cuotasTotales,
                bank, lastCreditPayment, items, null);
    }
}
