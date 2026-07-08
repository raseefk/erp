package com.supererp.erp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration — single-tenant mode.
 * Tenant-scoped caches replaced by application-level caches.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats());

        manager.setCacheNames(List.of(
            "tenantFeatures",      // application-level feature toggles
            "tenantMenus",         // application-level menu toggles
            "permissionManifest",  // full permission tree per user
            "tokenBlacklist"       // checked per-request
        ));

        return manager;
    }
}
