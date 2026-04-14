package api.m2.movements.services.currencies;

import api.m2.movements.exceptions.ExchangeRateNotFoundException;
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
     * en la fecha indicada.
     *
     * @param symbol el símbolo de la moneda (e.g., "ARS", "EUR")
     * @param date   la fecha para la cual obtener la tasa
     * @return la tasa de cambio
     * @throws ExchangeRateNotFoundException si no se puede obtener la tasa de cambio
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
                    .orElseThrow(() -> new ExchangeRateNotFoundException(
                            String.format("No se encontró tasa de cambio para %s en %s", symbol, date)));
        } catch (ExchangeRateNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener tasa de cambio para {} en {}: {}", symbol, date, e.getMessage());
            throw new ExchangeRateNotFoundException(symbol, date.toString(), e);
        }
    }
}
