package com.supererp.erp.rbac.service;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.entity.*;
import com.supererp.erp.rbac.repository.*;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final AppRoleRepository    roleRepo;
    private final PermissionRepository permRepo;
    private final TenantFeatureMappingRepository featureMapRepo;
    private final TenantMenuMappingRepository menuMapRepo;

    // ── Role Management ──────────────────────────────────────────────────────

    public List<AppRole> getRolesForCurrentTenant() {
        return getRoles(TenantContext.getTenantId());
    }

    public List<AppRole> getRoles(UUID tenantId) {
        return roleRepo.findByTenantIdOrderByNameAsc(tenantId);
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
        UUID tenantId = TenantContext.getTenantId();
        if (roleRepo.existsByTenantIdAndName(tenantId, name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }
        return roleRepo.save(AppRole.builder()
            .tenantId(tenantId)
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

        // Verify role belongs to current tenant
        if (!role.getTenantId().equals(TenantContext.getTenantId())) {
            throw new SecurityException("Access denied: role does not belong to current tenant");
        }

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
        if (!role.getTenantId().equals(TenantContext.getTenantId())) {
            throw new SecurityException("Access denied");
        }
        roleRepo.delete(role);
    }

    // ── Feature Toggle Management ────────────────────────────────────────────

    @Cacheable(value = "tenantFeatures", key = "#tenantId")
    public Set<String> getEnabledFeatures(UUID tenantId) {
        Set<String> enabled = new HashSet<>();
        featureMapRepo.findByTenantId(tenantId)
            .forEach(m -> { if (m.isEnabled()) enabled.add(m.getFeatureId()); });
        return enabled;
    }

    @Transactional
    @CacheEvict(value = {"tenantFeatures", "permissionManifest"}, allEntries = true)
    public void toggleFeature(UUID tenantId, String featureId, boolean enabled) {
        TenantFeatureMapping mapping = featureMapRepo
            .findById(new TenantFeatureId(tenantId, featureId))
            .orElse(TenantFeatureMapping.builder()
                .tenantId(tenantId)
                .featureId(featureId)
                .build());
        mapping.setEnabled(enabled);
        featureMapRepo.save(mapping);
    }

    public boolean isFeatureEnabled(String featureId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return true; // Super admins see everything or system context
        return getEnabledFeatures(tenantId).contains(featureId);
    }

    // ── Menu Toggle Management ────────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = {"tenantMenus"}, allEntries = true)
    public void toggleMenu(UUID tenantId, String menuId, boolean enabled) {
        TenantMenuMapping mapping = menuMapRepo
            .findById(new TenantMenuId(tenantId, menuId))
            .orElse(TenantMenuMapping.builder()
                .tenantId(tenantId)
                .menuId(menuId)
                .build());
        mapping.setEnabled(enabled);
        menuMapRepo.save(mapping);
    }

    /**
     * Returns true if the menu is enabled for the current tenant.
     * Default is ENABLED — a menu is only hidden when explicitly set to disabled.
     */
    @Cacheable(value = "tenantMenus", key = "T(com.supererp.erp.tenant.TenantContext).getTenantId() + '-' + #menuId")
    public boolean isMenuEnabled(String menuId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return true;
        // If a row exists with enabled=false, then it is disabled
        return !menuMapRepo.existsByTenantIdAndMenuIdAndEnabledFalse(tenantId, menuId);
    }

    public List<TenantMenuMapping> getMenuMappingsForTenant(UUID tenantId) {
        return menuMapRepo.findByTenantId(tenantId);
    }

    // ── Permission Query ─────────────────────────────────────────────────────

    public List<Permission> getAllPermissions() {
        return permRepo.findAll();
    }
}
