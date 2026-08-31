package api.m2.movements.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
public class CacheConfiguration {

    public static final String CURRENCY_CACHE = "currency";
    public static final String YAHOO_PRICE_CACHE = "yahooPrice";
    // Cachea las dos llamadas a api-identity que se disparan en casi cada request (resolver el
    // usuario autenticado, verificar membership de un workspace) — antes cada operación hacía un
    // round-trip HTTP síncrono sin cache, sin timeout y sin circuit breaker: si api-identity se
    // colgaba un momento, ninguna operación de esta app podía completarse aunque el JWT del
    // caller siguiera siendo válido. 5hs es deliberadamente generoso: cambios de membership o de
    // rol pueden tardar hasta ese tiempo en reflejarse acá, a cambio de sacar a api-identity del
    // camino crítico de casi todo el tráfico.
    public static final String IDENTITY_CACHE = "identity";

    private static final String KEY_PREFIX = "api-movements:";
    private static final Duration CURRENCY_TTL = Duration.ofHours(5);
    private static final Duration YAHOO_PRICE_TTL = Duration.ofHours(1);
    private static final Duration IDENTITY_TTL = Duration.ofHours(5);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = baseCacheConfig().disableCachingNullValues();
        // IDENTITY_CACHE NO puede deshabilitar el cacheo de nulls: verifyUserIsMemberOfWorkspace es
        // @Cacheable pero retorna void, y el mecanismo de cacheo de métodos void de Spring termina
        // pasando por el mismo chequeo de null que RedisCache usa para valores reales — con
        // disableCachingNullValues() activo, la primera invocación exitosa explota con
        // "Cache 'identity' does not allow 'null' values" en vez de cachear. Spring Data Redis
        // soporta cachear null nativamente (guarda un sentinel serializado), así que dejarlo
        // habilitado acá es seguro y no cambia el comportamiento de getMe()/getMe(Long), que
        // siempre devuelven un valor real.
        RedisCacheConfiguration identityConfig = baseCacheConfig();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CURRENCY_CACHE, defaultConfig.entryTtl(CURRENCY_TTL))
                .withCacheConfiguration(YAHOO_PRICE_CACHE, defaultConfig.entryTtl(YAHOO_PRICE_TTL))
                .withCacheConfiguration(IDENTITY_CACHE, identityConfig.entryTtl(IDENTITY_TTL))
                .build();
    }

    private static RedisCacheConfiguration baseCacheConfig() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        var serializer = GenericJacksonJsonRedisSerializer.create(
                builder -> builder.enableDefaultTyping(typeValidator)
                        // Permite deserializar entradas cacheadas antes de agregar un nuevo campo
                        // primitivo a una entidad (ej: Currency.isDefault): sin esto, Jackson
                        // rompe con SerializationException al mapear el JSON viejo (sin el campo).
                        .customize(mapperBuilder -> mapperBuilder.disable(
                                DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)));

        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> KEY_PREFIX + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
