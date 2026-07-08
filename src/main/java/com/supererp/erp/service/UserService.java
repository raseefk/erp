package com.supererp.erp.service;

import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.service.RbacService;
import com.supererp.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User management service — single-tenant mode.
 * All users belong to the single application; no tenant scoping.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final RbacService       rbacService;

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        return userRepository.findAllWithRoles();
    }

    @Transactional(readOnly = true)
    public AppUser getById(Long id) {
        return userRepository.findByIdWithRoles(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public AppUser getByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public void createUser(AppUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + user.getUsername());
        }
        user.setTenantId(AppTenantConfig.APP_TENANT_ID);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        log.info("Created user '{}'", user.getUsername());
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = getByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void toggleStatus(Long id) {
        AppUser user = getById(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    @Transactional
    public void updateUser(Long id, AppUser details, Long roleId, String newPassword) {
        AppUser user = getById(id);
        user.setFullName(details.getFullName());
        user.setEmail(details.getEmail());
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.trim().length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }
        if (roleId != null) {
            user.getRoles().clear();
            rbacService.getRole(roleId).ifPresent(role -> user.getRoles().add(role));
        }
        userRepository.save(user);
    }
}
