package com.supererp.erp.repository;

import com.supererp.erp.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for application users in single-tenant mode.
 * Tenant-scoped methods still exist for backward compatibility but operate
 * on the single fixed APP_TENANT_ID.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsername(String username);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsernameAndTenantId(String username, UUID tenantId);

    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithRoles(@Param("id") Long id);

    boolean existsByUsername(String username);
    boolean existsByUsernameAndTenantId(String username, UUID tenantId);

    @EntityGraph(attributePaths = {"roles"})
    List<AppUser> findAllByEnabledTrueOrderByFullNameAsc();

    List<AppUser> findAllByTenantIdAndEnabledTrueOrderByFullNameAsc(UUID tenantId);

    long countByTenantId(UUID tenantId);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    @Query("SELECT u FROM AppUser u")
    List<AppUser> findAllWithRoles();

    @Query("SELECT u.tenantId, COUNT(u) FROM AppUser u GROUP BY u.tenantId")
    List<Object[]> countUsersGroupedByTenant();
}
