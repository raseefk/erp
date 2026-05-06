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
    @Query("SELECT u FROM AppUser u WHERE u.id = :id")
    Optional<AppUser> findByIdWithRoles(@org.springframework.data.repository.query.Param("id") Long id);

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions " +
           "WHERE u.username = :username AND u.tenantId = :tenantId")
    Optional<AppUser> findByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);
    boolean existsByUsername(String username);

    java.util.List<AppUser> findAllByTenantIdAndEnabledTrueOrderByFullNameAsc(UUID tenantId);
    long countByTenantId(UUID tenantId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM AppUser u WHERE u.tenantId = :tenantId")
    java.util.List<AppUser> findAllWithRoles(@org.springframework.data.repository.query.Param("tenantId") UUID tenantId);
}
