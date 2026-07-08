package com.supererp.erp.security;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.entity.Permission;
import com.supererp.erp.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-tenant UserDetailsService.
 * Loads application users by username from app_users.
 * The SYSTEM_ADMIN concept (separate system_users table) has been unified:
 * admin users are just AppUsers with the ADMIN role.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // Add permission-based authorities
        user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getId)
            .distinct()
            .forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));

        // Add role-based authorities
        user.getRoles().forEach(r -> {
            String roleName = r.getName();
            if (!roleName.startsWith("ROLE_")) {
                roleName = "ROLE_" + roleName;
            }
            authorities.add(new SimpleGrantedAuthority(roleName));
        });

        boolean isAdmin = user.getRoles().stream().anyMatch(com.supererp.erp.rbac.entity.AppRole::isSystem);

        return new SecurityUser(
            user.getUsername(), user.getPassword(), user.isEnabled(),
            authorities, null, isAdmin);
    }

    @Transactional(readOnly = true)
    public AppUser loadAppUser(String username) {
        return userRepo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Backward-compatible overload — tenantId parameter is ignored.
     */
    @Transactional(readOnly = true)
    public AppUser loadAppUser(String username, java.util.UUID tenantId) {
        return loadAppUser(username);
    }
}
