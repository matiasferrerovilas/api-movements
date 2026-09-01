package api.m2.movements.records.movements;

import api.m2.movements.constraints.ValidCuotas;
import api.m2.movements.records.categories.CategoryUpdateRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
        LocalDate lastCreditPayment
) { }
