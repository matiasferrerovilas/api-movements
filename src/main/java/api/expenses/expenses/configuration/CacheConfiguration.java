package api.expenses.expenses.configuration;

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

    @Bean
    public CacheManager cacheManager() {
        List<Cache> caches = List.of(createCache(CURRENCY_CACHE, 3));
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(caches);
        return  cacheManager;
    }

    private static CaffeineCache createCache(String cacheName, int durationTime){
        return new CaffeineCache(cacheName, Caffeine.newBuilder()
                .expireAfterWrite(durationTime, TimeUnit.HOURS)
                .build());
    }
}
