package api.m2.movements.services.currencies;

import api.m2.movements.configuration.CacheConfiguration;
import api.m2.movements.entities.commons.Currency;
import api.m2.movements.exceptions.BusinessException;
import api.m2.movements.repositories.CurrencyRepository;
import api.m2.movements.exceptions.EntityNotFoundException;
import api.m2.movements.exceptions.ServiceException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

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

    @Cacheable(cacheNames = CacheConfiguration.CURRENCY_CACHE, key = "#symbol.trim().toUpperCase()")
    public Currency findBySymbol(@NotNull(message = "Debe indicar un tipo de moneda") String symbol) {
        var normalizedSymbol = symbol.trim().toUpperCase();

        return currencyRepository.findBySymbol(normalizedSymbol)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Currency not found: " + normalizedSymbol
                ));
    }

    @Cacheable(cacheNames = CacheConfiguration.CURRENCY_CACHE, key = "'DEFAULT_CURRENCY'")
    public Currency getDefaultCurrency() {
        var defaultCurrencies = currencyRepository.findAllByIsDefaultTrue();
        if (defaultCurrencies.isEmpty()) {
            throw new ServiceException("No hay ninguna moneda configurada como default");
        }
        if (defaultCurrencies.size() > 1) {
            log.warn("Hay {} monedas marcadas como default, se usa la primera: {}",
                    defaultCurrencies.size(), defaultCurrencies.getFirst().getSymbol());
        }

        return defaultCurrencies.getFirst();
    }
}