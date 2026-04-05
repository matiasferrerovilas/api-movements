package api.m2.movements.services.currencies;

import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.records.currencies.ExchangeRateRecord;
import api.m2.movements.records.movements.MovementRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static api.m2.movements.configuration.CacheConfiguration.CURRENCY_CACHE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final FrankfurterClient frankfurterClient;

    @Cacheable(cacheNames = CURRENCY_CACHE, key = "'rates_' + #base + '_' + #quotes")
    public List<ExchangeRateRecord> getRates(String base, String quotes) {
        return frankfurterClient.getRates(base, quotes);
    }

    @Cacheable(cacheNames = CURRENCY_CACHE, key = "'rates_' + #base + '_' + #quotes + '_' + #date")
    public List<ExchangeRateRecord> getRatesOnDate(String base, String quotes, LocalDate date) {
        var dateStr = date.toString();
        return frankfurterClient.getRatesOnDate(base, quotes, dateStr, dateStr);
    }

    public BigDecimal convertToUsd(String fromSymbol, BigDecimal amount) {
        if ("USD".equalsIgnoreCase(fromSymbol)) {
            return amount;
        }
        var rates = this.getRates("USD", fromSymbol.toUpperCase());
        return rates.stream()
                .filter(r -> fromSymbol.equalsIgnoreCase(r.quote()))
                .findFirst()
                .map(r -> amount.divide(r.rate(), 2, RoundingMode.HALF_UP))
                .orElseThrow(() -> new BusinessException(
                        "Cotización no disponible para: " + fromSymbol));
    }

    public MovementRecord enrich(MovementRecord record) {
        BigDecimal rate = record.exchangeRate();

        if (rate == null) {
            try {
                var rates = this.getRates("USD", record.currency().symbol().toUpperCase());
                rate = rates.stream()
                        .filter(r -> record.currency().symbol().equalsIgnoreCase(r.quote()))
                        .findFirst()
                        .map(ExchangeRateRecord::rate)
                        .orElse(null);
            } catch (Exception e) {
                log.warn("No se pudo obtener cotización para {}: {}",
                        record.currency().symbol(), e.getMessage());
            }
        }

        BigDecimal amountUsd = null;
        if (rate != null && rate.compareTo(BigDecimal.ZERO) != 0) {
            if ("USD".equalsIgnoreCase(record.currency().symbol())) {
                amountUsd = record.amount();
            } else {
                amountUsd = record.amount().divide(rate, 2, RoundingMode.HALF_UP);
            }
        }

        return new MovementRecord(
                record.id(),
                record.amount(),
                record.description(),
                record.date(),
                record.createdAt(),
                record.updatedAt(),
                record.category(),
                record.currency(),
                record.bank(),
                record.type(),
                record.owner(),
                record.account(),
                record.cuotaActual(),
                record.cuotasTotales(),
                rate,
                amountUsd
        );
    }
}
