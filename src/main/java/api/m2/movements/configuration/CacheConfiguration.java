package api.m2.movements.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfiguration {

    public static final String CURRENCY_CACHE = "currency";
    public static final String YAHOO_PRICE_CACHE = "yahooPrice";
    private static final int DURATION_TIME_DEFAULT = 5;
    private static final int YAHOO_CACHE_HOURS = 1;

    @Bean
    public CacheManager cacheManager() {
        List<Cache> caches = List.of(
                createCache(CURRENCY_CACHE, DURATION_TIME_DEFAULT),
                createCache(YAHOO_PRICE_CACHE, YAHOO_CACHE_HOURS));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private static CaffeineCache createCache(String cacheName, int durationTime) {
        return new CaffeineCache(cacheName, Caffeine.newBuilder()
                .expireAfterWrite(durationTime, TimeUnit.HOURS)
                .build());
    }
}
