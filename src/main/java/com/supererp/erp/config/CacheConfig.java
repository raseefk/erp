package com.supererp.erp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine cache configuration.
 * Uses Spring Cache abstraction — can be swapped to Redis
 * by replacing this config + adding spring-boot-starter-data-redis dependency.
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

        // Register named caches with different TTLs
        manager.setCacheNames(java.util.List.of(
            "tenantBySlug",       // 10 min — tenant metadata
            "tenantById",         // 10 min
            "tenantFeatures",     // 5 min — feature toggles per tenant
            "permissionManifest", // 5 min — full permission tree per user
            "tokenBlacklist"      // checked per-request
        ));

        return manager;
    }
}
