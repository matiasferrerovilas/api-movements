package api.m2.movements.unit.services

import api.m2.movements.configuration.CacheConfiguration
import api.m2.movements.services.workspaces.IdentityCacheInvalidator
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import spock.lang.Specification

class IdentityCacheInvalidatorTest extends Specification {

    def cacheManager = new ConcurrentMapCacheManager(
            CacheConfiguration.IDENTITY_CACHE, CacheConfiguration.WORKSPACE_ROLE_CACHE, CacheConfiguration.NAMES_CACHE)
    IdentityCacheInvalidator invalidator = new IdentityCacheInvalidator(cacheManager)

    def "invalidateAuthorization - should drop the cached membership and role"() {
        given:
        cacheManager.getCache(CacheConfiguration.IDENTITY_CACHE).put("membership:5:12", "granted")
        cacheManager.getCache(CacheConfiguration.WORKSPACE_ROLE_CACHE).put("me:a@a.com:5", "OWNER")

        when:
        invalidator.invalidateAuthorization()

        then:
        cacheManager.getCache(CacheConfiguration.IDENTITY_CACHE).get("membership:5:12") == null
        cacheManager.getCache(CacheConfiguration.WORKSPACE_ROLE_CACHE).get("me:a@a.com:5") == null
    }

    def "invalidateAuthorization - should leave the display-name cache alone"() {
        given:
        cacheManager.getCache(CacheConfiguration.NAMES_CACHE).put("workspace:5", "Casa")

        when:
        invalidator.invalidateAuthorization()

        then:
        cacheManager.getCache(CacheConfiguration.NAMES_CACHE).get("workspace:5").get() == "Casa"
    }
}
