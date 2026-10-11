package api.m2.movements.helpers;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Request factory con timeouts explícitos para los {@code RestClient} armados a mano. El
 * builder autoconfigurado de Spring Boot toma {@code spring.http.client.*}, pero
 * {@code RestClient.builder()} no, y sin timeouts una dependencia colgada deja el request
 * esperando indefinidamente.
 */
public final class HttpTimeouts {

    private HttpTimeouts() {
    }

    public static JdkClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        // HTTP/1.1 fijo: por defecto el HttpClient del JDK intenta HTTP/2 y, sobre texto plano, lo
        // negocia con un "Upgrade: h2c" en la primera request de cada conexión nueva — un costo
        // sin beneficio para pocas llamadas chicas, y ruido en los tests (el mismo request se
        // registraba dos veces).
        var client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
