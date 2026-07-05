package com.supererp.erp.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlugAndActiveTrue(String slug);
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);

    /**
     * Find tenants whose trial/subscription has expired but are still marked active.
     * expiresAt is not null, is in the past, and is_active = true.
     */
    @Query("SELECT t FROM Tenant t WHERE t.active = true AND t.expiresAt IS NOT NULL AND t.expiresAt < :now")
    List<Tenant> findExpiredActiveTenants(@Param("now") OffsetDateTime now);
}
