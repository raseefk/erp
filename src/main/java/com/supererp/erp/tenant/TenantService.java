package com.supererp.erp.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;

    @Cacheable(value = "tenantBySlug", key = "#slug")
    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlugAndActiveTrue(slug);
    }

    @Cacheable(value = "tenantById", key = "#id")
    public Optional<Tenant> findById(UUID id) {
        return tenantRepository.findById(id);
    }

    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = {"tenantBySlug", "tenantById"}, allEntries = true)
    public Tenant create(Tenant tenant) {
        if (tenantRepository.existsBySlug(tenant.getSlug())) {
            throw new IllegalArgumentException("Tenant slug already exists: " + tenant.getSlug());
        }
        return tenantRepository.save(tenant);
    }

    @Transactional
    @CacheEvict(value = {"tenantBySlug", "tenantById"}, allEntries = true)
    public Tenant update(UUID id, Tenant updated) {
        Tenant existing = tenantRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        existing.setName(updated.getName());
        existing.setLogoUrl(updated.getLogoUrl());
        existing.setPrimaryColor(updated.getPrimaryColor());
        existing.setActive(updated.isActive());
        existing.setPlan(updated.getPlan());
        existing.setMaxUsers(updated.getMaxUsers());
        existing.setExpiresAt(updated.getExpiresAt());
        return tenantRepository.save(existing);
    }

    @Transactional
    @CacheEvict(value = {"tenantBySlug", "tenantById"}, allEntries = true)
    public void deactivate(UUID id) {
        tenantRepository.findById(id).ifPresent(t -> {
            t.setActive(false);
            tenantRepository.save(t);
        });
    }
}
