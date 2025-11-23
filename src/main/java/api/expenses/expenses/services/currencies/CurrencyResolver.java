package api.expenses.expenses.services.currencies;

import api.expenses.expenses.entities.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyResolver {
    private final CurrencyAddService currencyAddService;

    public Currency resolve(String symbol) {
        return currencyAddService.findBySymbol(symbol);
    }
}
