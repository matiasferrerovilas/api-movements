package api.m2.movements.services.currencies;

import api.m2.movements.records.currencies.ExchangeRateRecord;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.time.LocalDate;
import java.util.List;

@HttpExchange
public interface FrankfurterClient {

    @GetExchange("/v2/rates")
    List<ExchangeRateRecord> getRates(
            @RequestParam String base,
            @RequestParam String quotes
    );

    @GetExchange("/v2/rates")
    List<ExchangeRateRecord> getRatesByDateRange(
            @RequestParam String base,
            @RequestParam String quotes,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    );

    @GetExchange("/v2/rates")
    List<ExchangeRateRecord> getRatesOnDate(
            @RequestParam String base,
            @RequestParam String quotes,
            @RequestParam String from,
            @RequestParam String to
    );
}
