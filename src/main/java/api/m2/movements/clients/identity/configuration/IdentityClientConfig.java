package api.m2.movements.clients.identity.configuration;

import api.m2.movements.clients.identity.IdentityClient;
import api.m2.movements.exceptions.PermissionDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class IdentityClientConfig {

    private static final String SOURCE_SERVICE_HEADER = "X-Source-Service";
    private static final String SOURCE_SERVICE_NAME = "api-movements";
    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public IdentityClient identityClient(@Value("${identity.base-url}") String baseUrl) {
        var restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(SOURCE_SERVICE_HEADER, SOURCE_SERVICE_NAME)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + this.getCurrentToken());
                    return execution.execute(request, body);
                })
                .build();
        var adapter = RestClientAdapter.create(restClient);
        var factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(IdentityClient.class);
    }

    private String getCurrentToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }

        throw new PermissionDeniedException("Usuario no autenticado");
    }
}
