package api.m2.movements.services.currencies;

import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.entities.commons.Currency;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.exceptions.EntityNotFoundException;
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

    public Currency addCurrency(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new BusinessException("El simbolo no peude estar vacio");
        }
        String normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> {
                    Currency newCurrency = Currency.builder()
                            .symbol(normalizedSymbol)
                            .description(normalizedSymbol)
                            .build();
                    return currencyRepository.save(newCurrency);
                });
    }

    public Currency addCurrency(String symbol, String description) {
        if (symbol == null || symbol.isBlank()) {
            throw new BusinessException("El símbolo no puede estar vacío");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessException("La descripción no puede estar vacía");
        }
        String normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseGet(() -> currencyRepository.save(Currency.builder()
                        .symbol(normalizedSymbol)
                        .description(description.trim())
                        .build()));
    }

    @Cacheable(cacheNames = CacheConfiguration.CURRENCY_CACHE)
    public List<Currency> getDefaultCurrencies() {
        return currencyRepository.findAllByEnabled(true);
    }

    @Cacheable(cacheNames = CacheConfiguration.CURRENCY_CACHE, key = "#symbol.trim().toUpperCase()")
    public Currency findBySymbol(@NotNull(message = "Debe indicar un tipo de moneda") String symbol) {
        var normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Currency not found: " + normalizedSymbol
                ));
    }
}