package api.expenses.expenses.services.currencies;

import api.expenses.expenses.configuration.CacheConfiguration;
import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.mappers.CurrencyMapper;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyAddService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    public Currency addCurrency(String symbol) {
        final String normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> {
                    Currency newCurrency = Currency.builder()
                            .symbol(normalizedSymbol)
                            .description(normalizedSymbol)
                            .build();
                    return currencyRepository.save(newCurrency);
                });
    }

  @Cacheable(cacheNames = CacheConfiguration.CURRENCY_CACHE)
  public List<CurrencyRecord> getAllCurrencies() {
        return currencyMapper.toRecordList(currencyRepository.findAllByEnabled(true));
    }

    public Currency findBySymbol(@NotNull(message = "Debe indicar un tipo de moneda") String symbol) {
        var normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Currency not found: " + normalizedSymbol
                ));
    }
}
