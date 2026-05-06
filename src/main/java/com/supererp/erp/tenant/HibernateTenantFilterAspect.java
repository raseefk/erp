package com.supererp.erp.tenant;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect to automatically enable the 'tenantFilter' on the Hibernate session
 * before any repository or service method executes.
 *
 * Also sets the 'app.current_tenant_id' and 'app.current_user_id'
 * session variables in PostgreSQL for Row-Level Security (RLS).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class HibernateTenantFilterAspect {

    private final EntityManager entityManager;

    @Pointcut("execution(* com.supererp.erp.repository..*(..))")
    public void repositoryMethods() {}

    @Pointcut("execution(* com.supererp.erp.rbac.repository..*(..))")
    public void rbacRepositoryMethods() {}

    @Pointcut("execution(* com.supererp.erp.service..*(..))")
    public void serviceMethods() {}

    @Before("repositoryMethods() || rbacRepositoryMethods() || serviceMethods()")
    public void beforeExecution() {
        UUID tenantId = TenantContext.getTenantId();
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "anonymous";
        
        log.info("RLS Aspect: [Tenant: {}] [User: {}] [Thread: {}]", 
                tenantId != null ? tenantId : "NONE", 
                username,
                Thread.currentThread().getName());
        
        Session session = entityManager.unwrap(Session.class);
        boolean isSystemAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));

        // 1. Enable/Disable Hibernate Filter
        if (tenantId != null) {
            log.debug("HibernateTenantFilterAspect: Enabling 'tenantFilter' for tenantId: {}", tenantId);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        } else {
            session.disableFilter("tenantFilter");
        }

        // 2. Set PostgreSQL session variables for RLS (Always set to prevent leak from previous pooled connections)
        try {
            // Set Tenant ID (Use session-level 'false' so it persists into the transaction)
            entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, false)")
                        .setParameter("tid", tenantId != null ? tenantId.toString() : "")
                        .getSingleResult();
            
            // Set System Admin bypass flag
            entityManager.createNativeQuery("SELECT set_config('app.is_system_admin', :isSys, false)")
                        .setParameter("isSys", isSystemAdmin ? "true" : "false")
                        .getSingleResult();
 
            // Set User ID if available
            if (auth instanceof com.supererp.erp.security.jwt.JwtAuthToken jwtAuth) {
                entityManager.createNativeQuery("SELECT set_config('app.current_user_id', :uid, false)")
                            .setParameter("uid", jwtAuth.getUserId().toString())
                            .getSingleResult();
            } else {
                entityManager.createNativeQuery("SELECT set_config('app.current_user_id', '', false)")
                            .getSingleResult();
            }
        } catch (Exception e) {
            log.warn("Could not set PostgreSQL session variables for RLS: {}", e.getMessage());
        }
    }
}
