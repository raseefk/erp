package com.supererp.erp.tenant;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.entity.AppRole;
import com.supererp.erp.rbac.repository.AppRoleRepository;
import com.supererp.erp.rbac.repository.PermissionRepository;
import com.supererp.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final AppRoleRepository appRoleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final jakarta.persistence.EntityManager entityManager;

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
    public void createWithAdmin(Tenant tenant, String adminUsername, String adminPassword, String adminFullName, String adminEmail) {
        // 1. Create the tenant
        Tenant savedTenant = create(tenant);
        UUID tenantId = savedTenant.getId();

        // 2. Provision the first administrator and role
        // Fetch permissions while STILL in the System Admin context (null tenant)
        // This ensures the Hibernate filter is NOT active during the fetch.
        List<com.supererp.erp.rbac.entity.Permission> allPermissions = permissionRepository.findAll();
        log.info("TenantService: Found {} global permissions BEFORE context switch", allPermissions.size());

        UUID previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(tenantId);
            
            // Create the ADMIN role
            AppRole adminRole = appRoleRepository.save(AppRole.builder()
                .tenantId(tenantId)
                .name("ADMIN")
                .description("Full System Administrator")
                .system(true)
                .build());

            // Use native SQL to ensure permissions are assigned even if Hibernate association is tricky
            log.info("TenantService: Assigning permissions to role ID {} via native SQL", adminRole.getId());
            entityManager.createNativeQuery(
                "INSERT INTO role_permissions (role_id, permission_id) " +
                "SELECT :roleId, id FROM permissions")
                .setParameter("roleId", adminRole.getId())
                .executeUpdate();

            // Create the first AppUser
            AppUser adminUser = AppUser.builder()
                .tenantId(tenantId)
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .email(adminEmail)
                .enabled(true)
                .roles(new HashSet<>(Set.of(adminRole)))
                .build();
            
            appUserRepository.save(adminUser);
            
            log.info("Successfully provisioned tenant {} with admin user {}", tenantId, adminUsername);
            
        } finally {
            // Restore original tenant context (usually null or SYSTEM)
            TenantContext.setTenantId(previousTenant);
        }
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
        existing.setMaxStorageGb(updated.getMaxStorageGb());
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
