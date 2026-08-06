package api.m2.movements.services.currencies;

import api.m2.movements.entities.commons.Currency;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyResolver {
    private final CurrencyAddService currencyAddService;
    private final WorkspaceCurrencyService workspaceCurrencyService;

    public Currency resolve(String symbol, Long workspaceId) {
        var currency = currencyAddService.findBySymbol(symbol);
        workspaceCurrencyService.ensureCurrencyInWorkspace(workspaceId, currency);
        return currency;
    }
}
