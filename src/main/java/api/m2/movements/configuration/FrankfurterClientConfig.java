package api.m2.movements.configuration;

import api.m2.movements.clients.rates.FrankfurterClient;
import api.m2.movements.helpers.HttpTimeouts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class FrankfurterClientConfig {

    @Bean
    public FrankfurterClient frankfurterClient(@Value("${frankfurter.base-url}") String baseUrl,
                                               @Value("${frankfurter.connect-timeout:3s}") Duration connectTimeout,
                                               @Value("${frankfurter.read-timeout:10s}") Duration readTimeout) {
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(HttpTimeouts.requestFactory(connectTimeout, readTimeout))
                .build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(FrankfurterClient.class);
    }
}
