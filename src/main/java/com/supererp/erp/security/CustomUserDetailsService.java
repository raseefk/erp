package com.supererp.erp.security;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.rbac.entity.Permission;
import com.supererp.erp.repository.AppUserRepository;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new UsernameNotFoundException("No tenant context for user: " + username);
        }
        AppUser user = userRepo.findByUsernameAndTenantId(username, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username + " in tenant: " + tenantId));

        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getId)
            .distinct()
            .map(p -> new SimpleGrantedAuthority("PERM_" + p))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(), user.getPassword(), user.isEnabled(),
            true, true, true, authorities);
    }

    @Transactional(readOnly = true)
    public AppUser loadAppUser(String username, UUID tenantId) {
        return userRepo.findByUsernameAndTenantId(username, tenantId)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found: " + username));
    }
}
