package api.m2.movements.investment.services.valuation;

import api.m2.movements.configuration.CacheConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class YahooFinanceClient {

    private static final String BASE_URL = "https://query1.finance.yahoo.com";

    private final RestClient restClient;

    public YahooFinanceClient() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
    }

    @Cacheable(value = CacheConfiguration.YAHOO_PRICE_CACHE, key = "#symbol")
    public BigDecimal getPrice(String symbol) {
        try {
            var response = restClient.get()
                    .uri("/v8/finance/chart/{symbol}?interval=1d&range=1d", symbol)
                    .retrieve()
                    .body(YahooChartResponse.class);

            if (response == null || response.chart() == null
                    || response.chart().result() == null
                    || response.chart().result().isEmpty()) {
                log.warn("Respuesta vacía de Yahoo Finance para símbolo: {}", symbol);
                return null;
            }

            return response.chart().result().getFirst().meta().regularMarketPrice();
        } catch (Exception e) {
            log.warn("Error consultando Yahoo Finance para {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YahooChartResponse(Chart chart) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Chart(List<ChartResult> result) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ChartResult(Meta meta) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Meta(BigDecimal regularMarketPrice, String currency) { }
    }
}
