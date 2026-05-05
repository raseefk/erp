package com.supererp.erp.repository;

import com.supererp.erp.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    @Query("SELECT u FROM AppUser u LEFT JOIN FETCH u.roles r LEFT JOIN FETCH r.permissions " +
           "WHERE u.username = :username AND u.tenantId = :tenantId")
    Optional<AppUser> findByUsernameAndTenantId(String username, UUID tenantId);

    boolean existsByUsernameAndTenantId(String username, UUID tenantId);
    boolean existsByUsername(String username);

    long countByTenantId(UUID tenantId);
}
