package com.supererp.erp.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Custom UserDetails implementation that carries tenant information within the security context.
 */
@Getter
public class SecurityUser extends User {
    private final UUID tenantId;
    private final boolean systemAdmin;

    public SecurityUser(String username, String password, boolean enabled, 
                        Collection<? extends GrantedAuthority> authorities, 
                        UUID tenantId, boolean systemAdmin) {
        super(username, password, enabled, true, true, true, authorities);
        this.tenantId = tenantId;
        this.systemAdmin = systemAdmin;
    }
}
