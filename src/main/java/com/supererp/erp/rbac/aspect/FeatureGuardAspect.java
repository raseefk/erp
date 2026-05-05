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

    @Before("@annotation(requiresFeature) || @within(requiresFeature)")
    public void checkFeature(RequiresFeature requiresFeature) {
        String feature = requiresFeature.value();
        UUID tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            // Probably a system admin or public path, let it through if not tenant context
            return;
        }

        boolean enabled = rbacService.getEnabledFeatures(tenantId).contains(feature);

        if (!enabled) {
            log.warn("Feature Blocked: Tenant {} tried to access disabled feature {}", tenantId, feature);
            throw new AccessDeniedException("This feature is not enabled for your subscription: " + feature);
        }
    }
}
