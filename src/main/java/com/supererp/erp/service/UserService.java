package com.supererp.erp.service;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.supererp.erp.tenant.TenantService tenantService;
    private final com.supererp.erp.rbac.service.RbacService rbacService;

    @Transactional(readOnly = true)
    public List<AppUser> getAllUsers() {
        java.util.UUID tenantId = com.supererp.erp.tenant.TenantContext.getTenantId();
        return userRepository.findAllWithRoles(tenantId);
    }

    @Transactional(readOnly = true)
    public AppUser getById(Long id) {
        java.util.UUID tenantId = com.supererp.erp.tenant.TenantContext.getTenantId();
        return userRepository.findByIdWithRolesAndTenant(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public AppUser getByUsername(String username) {
        java.util.UUID tenantId = com.supererp.erp.tenant.TenantContext.getTenantId();
        log.info("UserService: Looking for user '{}' in tenant '{}'", username, tenantId);
        return userRepository.findByUsernameAndTenantId(username, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void createUser(AppUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        
        java.util.UUID tenantId = com.supererp.erp.tenant.TenantContext.getTenantId();
        if (tenantId != null) {
            com.supererp.erp.tenant.Tenant tenant = tenantService.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
            long currentUsers = userRepository.countByTenantId(tenantId);
            if (currentUsers >= tenant.getMaxUsers()) {
                throw new IllegalArgumentException("User limit exceeded. Maximum allowed users for this organization is " + tenant.getMaxUsers());
            }
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = getByUsername(username);
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
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
                throw new IllegalArgumentException("New password must be at least 6 characters long");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (roleId != null) {
            // Update role if changed
            user.getRoles().clear();
            rbacService.getRole(roleId).ifPresent(role -> user.getRoles().add(role));
        }
        
        userRepository.save(user);
    }
}
