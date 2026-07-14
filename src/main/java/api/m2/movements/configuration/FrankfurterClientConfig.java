package api.m2.movements.configuration;

import api.m2.movements.services.currencies.FrankfurterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class FrankfurterClientConfig {

    @Bean
    public FrankfurterClient frankfurterClient(@Value("${frankfurter.base-url}") String baseUrl) {
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(FrankfurterClient.class);
    }
}
