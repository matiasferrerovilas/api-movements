package api.m2.movements.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

@AllArgsConstructor
@Getter
public enum BanksEnum {
    GALICIA("GALICIA"),
    BBVA("BBVA"),
    SANTANDER_RIO("SANTANDER RIO"),
    BANCO_CIUDAD("BANCO CIUDAD");

    private final String description;

    public static BanksEnum findByDescription(String description) {
        if (!StringUtils.hasText(description)) {
            throw new IllegalArgumentException("La descripción del banco no puede estar vacía");
        }

        String normalized = description
                .trim()
                .toUpperCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(bank -> bank.description.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Banco no válido: " + description
                        )
                );
    }
}
