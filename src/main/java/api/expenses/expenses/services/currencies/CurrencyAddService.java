package api.expenses.expenses.services.currencies;

import api.expenses.expenses.entities.Currency;
import api.expenses.expenses.mappers.CurrencyMapper;
import api.expenses.expenses.records.currencies.CurrencyRecord;
import api.expenses.expenses.repositories.CurrencyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CurrencyAddService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    public Currency addCurrency(String currency){
        return currencyRepository.findBySymbol(currency)
                .orElseGet(() -> currencyRepository.save(Currency.builder().symbol(currency).description(currency).build()));
    }

    public List<CurrencyRecord> getAllCurrencies() {
        return currencyMapper.toRecordList(currencyRepository.findAll());
    }

    public Currency findBySymbol(@NotNull(message = "Debe indicar un tipo de moneda") String currency) {
        return currencyRepository.findBySymbol(currency)
                .orElseThrow(()-> new EntityNotFoundException("Currency not found"));
    }
}
