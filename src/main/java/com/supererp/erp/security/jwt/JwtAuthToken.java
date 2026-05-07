package com.supererp.erp.security.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

/**
 * Custom Authentication token that holds JWT-extracted user context.
 */
public class JwtAuthToken extends AbstractAuthenticationToken {

    private final String username;
    private final Long   userId;
    private final String tenantId;
    private final String rawToken;
    private final boolean systemRole;

    public JwtAuthToken(String username, Long userId, String tenantId,
                        Collection<? extends GrantedAuthority> authorities,
                        String rawToken, boolean systemRole) {
        super(authorities);
        this.username   = username;
        this.userId     = userId;
        this.tenantId   = tenantId;
        this.rawToken   = rawToken;
        this.systemRole = systemRole;
        setAuthenticated(true);
    }

    @Override public Object getPrincipal()   { return username; }
    @Override public Object getCredentials() { return rawToken; }
    public Long    getUserId()    { return userId; }
    public String  getTenantId()  { return tenantId; }
    public boolean isSystemRole() { return systemRole; }
}
