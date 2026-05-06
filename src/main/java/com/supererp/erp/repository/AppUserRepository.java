package com.supererp.erp.repository;

import com.supererp.erp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    Optional<AppUser> findByUsername(String username);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM AppUser u WHERE u.id = :id AND u.tenantId = :tenantId")
    Optional<AppUser> findByIdWithRolesAndTenant(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("tenantId") UUID tenantId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findByUsernameAndTenantId(String username, java.util.UUID tenantId);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);
    boolean existsByUsername(String username);

    java.util.List<AppUser> findAllByTenantIdAndEnabledTrueOrderByFullNameAsc(UUID tenantId);
    long countByTenantId(UUID tenantId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM AppUser u WHERE u.tenantId = :tenantId")
    java.util.List<AppUser> findAllWithRoles(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);
}
