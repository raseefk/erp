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
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.supererp.erp.tenant.TenantService tenantService;

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public AppUser getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public AppUser getByUsername(String username) {
        return userRepository.findByUsername(username)
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
}
