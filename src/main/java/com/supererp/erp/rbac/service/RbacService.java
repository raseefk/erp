package com.supererp.erp.rbac.service;

import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.rbac.entity.*;
import com.supererp.erp.rbac.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * RBAC service — single-tenant mode.
 * Feature/menu activation is application-level (not per-tenant).
 * All methods that previously took a tenantId parameter now use
 * AppTenantConfig.APP_TENANT_ID.
 */
@Service
@RequiredArgsConstructor
public class RbacService {

    private final AppRoleRepository    roleRepo;
    private final PermissionRepository permRepo;
    private final TenantFeatureMappingRepository featureMapRepo;
    private final TenantMenuMappingRepository menuMapRepo;

    // ── Role Management ──────────────────────────────────────────────────────

    /** Returns all application roles (single tenant). */
    public List<AppRole> getAllRoles() {
        return roleRepo.findAllByOrderByNameAsc();
    }

    /** Backward-compatible: returns all roles regardless of tenantId argument. */
    public List<AppRole> getRoles(UUID tenantId) {
        return roleRepo.findAllByOrderByNameAsc();
    }

    public Optional<AppRole> getRole(Long roleId) {
        return roleRepo.findById(roleId);
    }

    public Optional<AppRole> getRoleWithPermissions(Long roleId) {
        return roleRepo.findByIdWithPermissions(roleId);
    }

    @Transactional
    @CacheEvict(value = "permissionManifest", allEntries = true)
    public AppRole createRole(String name, String description) {
        if (roleRepo.existsByName(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        return roleRepo.save(AppRole.builder()
            .tenantId(AppTenantConfig.APP_TENANT_ID)
            .name(name)
            .description(description)
            .system(false)
            .build());
    }

    @Transactional
    @CacheEvict(value = "permissionManifest", allEntries = true)
    public AppRole updateRolePermissions(Long roleId, Set<String> permissionIds) {
        AppRole role = roleRepo.findByIdWithPermissions(roleId)
            .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        Set<Permission> newPerms = new HashSet<>(permRepo.findAllById(permissionIds));
        role.setPermissions(newPerms);
        return roleRepo.save(role);
    }

    @Transactional
    @CacheEvict(value = "permissionManifest", allEntries = true)
    public void deleteRole(Long roleId) {
        AppRole role = roleRepo.findById(roleId)
            .orElseThrow(() -> new NoSuchElementException("Role not found: " + roleId));
        if (role.isSystem()) {
            throw new IllegalStateException("Cannot delete system role: " + role.getName());
        }
        roleRepo.delete(role);
    }

    // ── Feature Toggle Management (Application-Level) ────────────────────────

    /**
     * Returns the set of enabled feature IDs for the application.
     * Uses APP_TENANT_ID as the key.
     */
    @Cacheable(value = "tenantFeatures", key = "'app'")
    @Transactional(readOnly = true)
    public Set<String> getEnabledFeatures() {
        Set<String> enabled = new HashSet<>();
        featureMapRepo.findByTenantId(AppTenantConfig.APP_TENANT_ID)
            .forEach(m -> { if (m.isEnabled()) enabled.add(m.getFeatureId()); });
        return enabled;
    }

    /** Backward-compatible overload — ignores tenantId, uses app-level. */
    @Cacheable(value = "tenantFeatures", key = "'app'")
    @Transactional(readOnly = true)
    public Set<String> getEnabledFeatures(UUID tenantId) {
        return getEnabledFeatures();
    }

    @Transactional
    @CacheEvict(value = {"tenantFeatures", "permissionManifest"}, allEntries = true)
    public void toggleFeature(String featureId, boolean enabled) {
        TenantFeatureMapping mapping = featureMapRepo
            .findById(new TenantFeatureId(AppTenantConfig.APP_TENANT_ID, featureId))
            .orElse(TenantFeatureMapping.builder()
                .tenantId(AppTenantConfig.APP_TENANT_ID)
                .featureId(featureId)
                .build());
        mapping.setEnabled(enabled);
        featureMapRepo.save(mapping);
    }

    /** Backward-compatible overload with tenantId — ignored. */
    @Transactional
    @CacheEvict(value = {"tenantFeatures", "permissionManifest"}, allEntries = true)
    public void toggleFeature(UUID tenantId, String featureId, boolean enabled) {
        toggleFeature(featureId, enabled);
    }

    public boolean isFeatureEnabled(String featureId) {
        return getEnabledFeatures().contains(featureId);
    }

    // ── Menu Toggle Management (Application-Level) ────────────────────────────

    @Transactional
    @CacheEvict(value = "tenantMenus", allEntries = true)
    public void toggleMenu(String menuId, boolean enabled) {
        TenantMenuMapping mapping = menuMapRepo
            .findById(new TenantMenuId(AppTenantConfig.APP_TENANT_ID, menuId))
            .orElse(TenantMenuMapping.builder()
                .tenantId(AppTenantConfig.APP_TENANT_ID)
                .menuId(menuId)
                .build());
        mapping.setEnabled(enabled);
        menuMapRepo.save(mapping);
    }

    /** Backward-compatible overload with tenantId — ignored. */
    @Transactional
    @CacheEvict(value = "tenantMenus", allEntries = true)
    public void toggleMenu(UUID tenantId, String menuId, boolean enabled) {
        toggleMenu(menuId, enabled);
    }

    /**
     * Returns true if the menu is enabled at the application level.
     * Default is ENABLED.
     */
    @Cacheable(value = "tenantMenus", key = "'app-' + #menuId")
    @Transactional(readOnly = true)
    public boolean isMenuEnabled(String menuId) {
        return !menuMapRepo.existsByTenantIdAndMenuIdAndEnabledFalse(
                AppTenantConfig.APP_TENANT_ID, menuId);
    }

    public List<TenantMenuMapping> getMenuMappingsForApplication() {
        return menuMapRepo.findByTenantId(AppTenantConfig.APP_TENANT_ID);
    }

    /** Backward-compatible overload. */
    public List<TenantMenuMapping> getMenuMappingsForTenant(UUID tenantId) {
        return getMenuMappingsForApplication();
    }

    // ── Permission Query ─────────────────────────────────────────────────────

    public List<Permission> getAllPermissions() {
        return permRepo.findAll();
    }
}
