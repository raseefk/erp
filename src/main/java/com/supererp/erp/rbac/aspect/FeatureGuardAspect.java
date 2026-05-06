package com.supererp.erp.rbac.aspect;

import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.service.RbacService;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureGuardAspect {

    private final RbacService rbacService;

    @Before("@annotation(com.supererp.erp.rbac.annotation.RequiresFeature) || @within(com.supererp.erp.rbac.annotation.RequiresFeature)")
    public void checkFeature(org.aspectj.lang.JoinPoint joinPoint) {
        RequiresFeature requiresFeature = null;
        
        // 1. Try Method
        if (joinPoint.getSignature() instanceof org.aspectj.lang.reflect.MethodSignature) {
            requiresFeature = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature())
                .getMethod().getAnnotation(RequiresFeature.class);
        }
        
        // 2. Try Class
        if (requiresFeature == null) {
            requiresFeature = joinPoint.getTarget().getClass().getAnnotation(RequiresFeature.class);
        }

        if (requiresFeature == null) return;

        String feature = requiresFeature.value();
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // Probably a system admin or public path, let it through if not tenant context
            return;
        }

        boolean enabled = rbacService.getEnabledFeatures(tenantId).contains(feature);

        if (!enabled) {
            log.warn("Feature Blocked: Tenant {} tried to access disabled feature {}", tenantId, feature);
            throw new com.supererp.erp.rbac.exception.FeatureDisabledException(feature);
        }
    }
}
