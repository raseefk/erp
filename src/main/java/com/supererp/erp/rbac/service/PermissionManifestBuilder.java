package com.supererp.erp.rbac.service;

import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.entity.*;
import com.supererp.erp.rbac.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the Permission Manifest — the complete authorized UI tree
 * returned to the client on login.
 *
 * In single-tenant mode, feature enablement is application-level.
 */
@Service
@RequiredArgsConstructor
public class PermissionManifestBuilder {

    private final FeatureRepository                  featureRepo;
    private final TenantFeatureMappingRepository     featureMapRepo;

    @Cacheable(value = "permissionManifest", key = "#user.id")
    public Map<String, Object> buildManifest(AppUser user) {
        // Collect all permissions this user has across all roles
        Set<String> userPermissions = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getId)
            .collect(Collectors.toSet());

        // Get enabled features for the application
        Set<String> enabledFeatures = featureMapRepo.findByTenantId(AppTenantConfig.APP_TENANT_ID).stream()
            .filter(TenantFeatureMapping::isEnabled)
            .map(TenantFeatureMapping::getFeatureId)
            .collect(Collectors.toSet());

        // Build feature tree
        List<Map<String, Object>> featureList = featureRepo.findAllOrdered().stream()
            .map(feature -> buildFeatureNode(feature, userPermissions, enabledFeatures))
            .collect(Collectors.toList());

        return Map.of(
            "userId",      user.getId(),
            "username",    user.getUsername(),
            "fullName",    user.getFullName(),
            "features",    featureList
        );
    }

    private Map<String, Object> buildFeatureNode(Feature feature,
                                                  Set<String> userPerms,
                                                  Set<String> enabledFeatures) {
        boolean featureEnabled = enabledFeatures.contains(feature.getId());

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id",          feature.getId());
        node.put("displayName", feature.getDisplayName());
        node.put("icon",        feature.getIcon());
        node.put("enabled",     featureEnabled);

        if (featureEnabled && feature.getMenus() != null) {
            List<Map<String, Object>> menus = feature.getMenus().stream()
                .map(menu -> buildMenuNode(menu, userPerms))
                .collect(Collectors.toList());
            node.put("menus", menus);
        } else {
            node.put("menus", List.of());
        }
        return node;
    }

    private Map<String, Object> buildMenuNode(Menu menu, Set<String> userPerms) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id",          menu.getId());
        node.put("displayName", menu.getDisplayName());
        node.put("url",         menu.getUrlPattern());
        node.put("icon",        menu.getIcon());

        Map<String, Boolean> permMap = new LinkedHashMap<>();
        if (menu.getPermissions() != null) {
            for (Permission perm : menu.getPermissions()) {
                permMap.put(perm.getAction(), userPerms.contains(perm.getId()));
            }
        }
        node.put("permissions", permMap);

        boolean hasAccess = permMap.values().stream().anyMatch(Boolean.TRUE::equals);
        node.put("accessible", hasAccess);

        return node;
    }
}
