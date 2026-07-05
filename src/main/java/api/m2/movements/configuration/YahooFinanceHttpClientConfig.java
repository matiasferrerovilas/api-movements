package api.m2.movements.configuration;

import api.m2.movements.investment.services.valuation.YahooFinanceHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class YahooFinanceHttpClientConfig {

    @Bean
    public YahooFinanceHttpClient yahooFinanceHttpClient(@Value("${yahoo-finance.base-url}") String baseUrl) {
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0")
                .build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(YahooFinanceHttpClient.class);
    }
}
