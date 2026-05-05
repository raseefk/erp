package com.supererp.erp.rbac.aspect;

import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.rbac.service.RbacService;
import com.supererp.erp.security.jwt.JwtAuthToken;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionCheckAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        String permission = requiresPermission.value();
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Full authentication is required to access this resource");
        }

        // System admins have all permissions
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            return;
        }

        // Check if user has the specific permission
        boolean hasPermission = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("PERM_" + permission));

        if (!hasPermission) {
            log.warn("Access Denied: User {} lacks permission {}", auth.getName(), permission);
            throw new AccessDeniedException("You do not have the required permission: " + permission);
        }
    }
}
