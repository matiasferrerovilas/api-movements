package api.m2.movements.investment.services.valuation;

import api.m2.movements.configuration.CacheConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class YahooFinanceClient {

    private final YahooFinanceHttpClient httpClient;

    @Cacheable(value = CacheConfiguration.YAHOO_PRICE_CACHE, key = "#symbol", unless = "#result.isEmpty()")
    public Optional<BigDecimal> getPrice(String symbol) {
        try {
            var response = httpClient.getChart(symbol);
            var results = Optional.ofNullable(response)
                    .map(YahooFinanceHttpClient.YahooChartResponse::chart)
                    .map(YahooFinanceHttpClient.YahooChartResponse.Chart::result)
                    .orElse(List.of());

            if (results.isEmpty()) {
                log.debug("Respuesta vacía de Yahoo Finance para símbolo: {}", symbol);
                return Optional.empty();
            }

            return Optional.ofNullable(results.getFirst().meta().regularMarketPrice());
        } catch (Exception e) {
            log.warn("Error consultando Yahoo Finance para {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }
}
