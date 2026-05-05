package com.supererp.erp.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlugAndActiveTrue(String slug);
    Optional<Tenant> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
