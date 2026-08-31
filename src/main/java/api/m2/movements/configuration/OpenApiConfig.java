package api.m2.movements.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // Antes hardcodeaba .version("2.3.0") acá, desincronizado de build.gradle apenas se bumpeaba
    // la versión ahí — BuildProperties lo lee de build-info.properties, generado en build time
    // por springBoot { buildInfo() } (ya configurado), así que no puede volver a desincronizarse.
    @Bean
    public OpenAPI customOpenAPI(BuildProperties buildProperties) {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .info(new Info()
                        .title("api-movements API")
                        .description("""
                                API para la gestión de finanzas personales.

                                **Funcionalidades:**
                                • Movimientos (ingresos/gastos) con importación desde PDF bancario
                                • Presupuestos con alertas por umbral
                                • Inversiones con valuación en vivo
                                • Control de suscripciones y servicios recurrentes
                                • Workspaces compartidos con invitaciones
                                • Balance por período, categoría y cuenta
                                • Tasas de cambio automáticas

                                **Autenticación:** JWT Bearer Token (OAuth2, Keycloak realm `m2`)
                                """)
                        .version(buildProperties.getVersion())
                        .contact(new Contact()
                                .name("API Support")
                                .email("api-support@movement.eva-core.com")));
    }
}
