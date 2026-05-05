package com.supererp.erp.repository;

import com.supererp.erp.entity.SystemUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findByUsernameAndEnabledTrue(String username);
    boolean existsByUsername(String username);
}
