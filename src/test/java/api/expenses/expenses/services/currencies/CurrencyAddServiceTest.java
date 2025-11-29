package api.expenses.expenses.services.currencies;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.mappers.CurrencyMapper;
import api.expenses.expenses.mappers.CurrencyMapperImpl;
import api.expenses.expenses.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyAddServiceTest {
    @Mock
    private CurrencyRepository currencyRepository;
    @Spy
    private CurrencyMapperImpl currencyMapper;
    @InjectMocks
    private CurrencyAddService currencyAddService;

    @Test
    @DisplayName("Agrego correctamente una moneda a la base de datos")
    void addCurrencyOk() {
        when(currencyRepository.findBySymbol("ARS"))
                .thenReturn(Optional.empty());

        var newCurrency = Currency.builder()
                .symbol("ARS")
                .description("ARS")
                .build();

        when(currencyRepository.save(any(Currency.class)))
                .thenReturn(newCurrency);

        Currency result = currencyAddService.addCurrency("ARS");

        assertEquals("ARS", result.getSymbol());
        verify(currencyRepository).save(any(Currency.class));
    }
    @Test
    @DisplayName("Debe devolver una moneda existente sin guardarla")
    void addCurrencyReturnsExisting() {
        var currency = Currency.builder()
                .symbol("ARS")
                .description("ARS")
                .build();

        when(currencyRepository.findBySymbol("ARS"))
                .thenReturn(Optional.of(currency));

        Currency result = currencyAddService.addCurrency("ARS");

        assertSame(currency, result);
        verify(currencyRepository, never()).save(any(Currency.class));
    }

    @Test
    @DisplayName("Debe devolver la Currency buscada por symbol normalizado")
    void findBySymbolOk() {
        Currency currency = Currency.builder().symbol("ARS").build();

        when(currencyRepository.findBySymbol("ARS"))
                .thenReturn(Optional.of(currency));

        Currency result = currencyAddService.findBySymbol("ars");

        assertEquals("ARS", result.getSymbol());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la Currency no existe")
    void findBySymbolNotFound() {
        when(currencyRepository.findBySymbol("ARS"))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> currencyAddService.findBySymbol("ARS"));
    }
}