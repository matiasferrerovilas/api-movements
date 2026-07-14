package api.m2.movements.configuration;

import api.m2.movements.clients.identity.IdentityClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class IdentityClientConfig {

    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";
    private static final String SOURCE_SERVICE_NAME = "api-movements";

    @Bean
    public IdentityClient identityClient(@Value("${identity.base-url}") String baseUrl) {
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(SOURCE_SERVICE_HEADER, SOURCE_SERVICE_NAME)
                .build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(IdentityClient.class);
    }
}
