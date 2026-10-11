package api.m2.movements.records.currencies;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import jakarta.validation.constraints.NotBlank;

@RegisterReflectionForBinding
public record CurrencyRecord(
        @NotBlank(message = "El símbolo de la moneda es requerido")
        String symbol,
        Long id) { }
