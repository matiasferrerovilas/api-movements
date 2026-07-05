package api.m2.movements.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

@Configuration
public class CacheConfiguration {

    public static final String CURRENCY_CACHE = "currency";
    public static final String YAHOO_PRICE_CACHE = "yahooPrice";
    private static final String KEY_PREFIX = "api-movements:";
    private static final Duration CURRENCY_TTL = Duration.ofHours(5);
    private static final Duration YAHOO_PRICE_TTL = Duration.ofHours(1);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = defaultCacheConfig();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(CURRENCY_CACHE, defaultConfig.entryTtl(CURRENCY_TTL))
                .withCacheConfiguration(YAHOO_PRICE_CACHE, defaultConfig.entryTtl(YAHOO_PRICE_TTL))
                .build();
    }

    private static RedisCacheConfiguration defaultCacheConfig() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        var serializer = GenericJacksonJsonRedisSerializer.create(
                builder -> builder.enableDefaultTyping(typeValidator));

        return RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> KEY_PREFIX + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();
    }
}
