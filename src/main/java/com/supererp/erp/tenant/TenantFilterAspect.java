package com.supererp.erp.tenant;

import com.supererp.erp.entity.TenantAwareEntity;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Aspect to automatically enable the Hibernate tenant filter for all database operations.
 * This ensures strict data isolation without requiring manual filter activation in services.
 */
@Aspect
@Component
public class TenantFilterAspect {

    @Autowired
    private EntityManager entityManager;

    /**
     * Activates the 'tenantFilter' on the Hibernate session before any repository method execution.
     */
    @Before("execution(* com.supererp.erp.repository..*(..))")
    public void enableTenantFilter() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Session session = entityManager.unwrap(Session.class);
            session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
        }
    }
}
