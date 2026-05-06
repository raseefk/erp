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
        log.debug("HibernateTenantFilterAspect: Resolved tenantId from context: {}", tenantId);
        
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            log.debug("HibernateTenantFilterAspect: Enabling 'tenantFilter' for tenantId: {}", tenantId);
            
            // 1. Enable Hibernate Filter
            session.enableFilter("tenantFilter")
                   .setParameter("tenantId", tenantId);
            
            // 2. Set PostgreSQL session variables for RLS
            try {
                entityManager.createNativeQuery("SELECT set_config('app.current_tenant_id', :tid, true)")
                            .setParameter("tid", tenantId.toString())
                            .getSingleResult();
                
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof com.supererp.erp.security.jwt.JwtAuthToken jwtAuth) {
                    entityManager.createNativeQuery("SELECT set_config('app.current_user_id', :uid, true)")
                                .setParameter("uid", jwtAuth.getUserId().toString())
                                .getSingleResult();
                }
            } catch (Exception e) {
                log.warn("Could not set PostgreSQL session variables for RLS: {}", e.getMessage());
            }
        } else {
            log.warn("HibernateTenantFilterAspect: No tenantId found in TenantContext! Data isolation might be compromised.");
        }
    }
}
