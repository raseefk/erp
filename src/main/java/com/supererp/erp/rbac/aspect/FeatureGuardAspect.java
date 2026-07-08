package com.supererp.erp.rbac.aspect;

import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Checks whether a feature is enabled at the application level before
 * allowing a controller method or class to execute.
 *
 * In single-tenant mode, feature toggles are global (not per-tenant).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureGuardAspect {

    private final RbacService rbacService;

    @Before("@annotation(com.supererp.erp.rbac.annotation.RequiresFeature) || @within(com.supererp.erp.rbac.annotation.RequiresFeature)")
    public void checkFeature(JoinPoint joinPoint) {
        RequiresFeature requiresFeature = null;

        // 1. Try Method
        if (joinPoint.getSignature() instanceof MethodSignature ms) {
            requiresFeature = ms.getMethod().getAnnotation(RequiresFeature.class);
        }

        // 2. Try Class
        if (requiresFeature == null) {
            requiresFeature = joinPoint.getTarget().getClass().getAnnotation(RequiresFeature.class);
        }

        if (requiresFeature == null) return;

        String feature = requiresFeature.value();

        // Admins always bypass feature guards
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_SYSTEM_ADMIN"))) {
            return;
        }

        boolean enabled = rbacService.isFeatureEnabled(feature);

        if (!enabled) {
            log.warn("Feature blocked: attempted access to disabled feature '{}'", feature);
            throw new com.supererp.erp.rbac.exception.FeatureDisabledException(feature);
        }
    }
}
