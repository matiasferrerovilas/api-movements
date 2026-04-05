package api.m2.movements.services.currencies;

import api.m2.movements.records.currencies.ExchangeRateRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateResolver {

    private final ExchangeRateService exchangeRateService;

    /**
     * Retorna la tasa de cambio: cuántas unidades de {@code symbol} equivalen a 1 USD,
     * en la fecha indicada. Retorna {@code null} si Frankfurter falla o no tiene la moneda.
     */
    public BigDecimal resolveRate(String symbol, LocalDate date) {
        if ("USD".equalsIgnoreCase(symbol)) {
            return BigDecimal.ONE;
        }
        try {
            var rates = exchangeRateService.getRatesOnDate("USD", symbol.toUpperCase(), date);
            return rates.stream()
                    .filter(r -> symbol.equalsIgnoreCase(r.quote()))
                    .findFirst()
                    .map(ExchangeRateRecord::rate)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("No se pudo obtener tasa de cambio para {} en {}: {}", symbol, date, e.getMessage());
            return null;
        }
    }
}
