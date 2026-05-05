package com.supererp.erp.controller.api;

import com.supererp.erp.rbac.entity.AppRole;
import com.supererp.erp.rbac.entity.Feature;
import com.supererp.erp.rbac.repository.FeatureRepository;
import com.supererp.erp.rbac.repository.PermissionRepository;
import com.supererp.erp.rbac.service.RbacService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolePermissionApiController {

    private final RbacService        rbacService;
    private final FeatureRepository  featureRepo;
    private final PermissionRepository permRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_VIEW')")
    public ResponseEntity<List<AppRole>> listRoles() {
        return ResponseEntity.ok(rbacService.getRolesForCurrentTenant());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public ResponseEntity<AppRole> createRole(@RequestBody CreateRoleRequest req) {
        return ResponseEntity.ok(rbacService.createRole(req.getName(), req.getDescription()));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_VIEW')")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long id) {
        return rbacService.getRoleWithPermissions(id)
            .map(role -> {
                Set<String> assigned = new HashSet<>();
                role.getPermissions().forEach(p -> assigned.add(p.getId()));
                List<Feature> features = featureRepo.findAllOrdered();
                return ResponseEntity.ok(Map.of(
                    "role",            Map.of("id", role.getId(), "name", role.getName()),
                    "assignedPerms",   assigned,
                    "featureTree",     features
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public ResponseEntity<AppRole> savePermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionIds) {
        return ResponseEntity.ok(rbacService.updateRolePermissions(id, permissionIds));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SETTINGS_ROLES_MANAGE')")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        rbacService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateRoleRequest {
        private String name;
        private String description;
    }
}
