package api.m2.movements.investment.services.valuation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.math.BigDecimal;
import java.util.List;

@HttpExchange
public interface YahooFinanceHttpClient {

    @GetExchange("/v8/finance/chart/{symbol}?interval=1d&range=1d")
    YahooChartResponse getChart(@PathVariable String symbol);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record YahooChartResponse(Chart chart) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Chart(List<ChartResult> result) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ChartResult(Meta meta) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Meta(BigDecimal regularMarketPrice, String currency) { }
    }
}
