package com.supererp.erp.security.jwt;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.entity.SystemUser;
import com.supererp.erp.rbac.entity.Permission;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        // Ensure at least 256 bits for HS256
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Generate access token for a tenant user */
    public String generateToken(AppUser user) {
        String jti = UUID.randomUUID().toString();
        List<String> permissions = user.getRoles().stream()
            .flatMap(r -> r.getPermissions().stream())
            .map(Permission::getId)
            .distinct()
            .collect(Collectors.toList());

        List<String> roles = user.getRoles().stream()
            .map(r -> {
                String name = r.getName();
                return name.startsWith("ROLE_") ? name : "ROLE_" + name;
            })
            .collect(Collectors.toList());

        return Jwts.builder()
            .id(jti)
            .subject(user.getUsername())
            .claim("user_id", user.getId())
            .claim("tenant_id", user.getTenantId().toString())
            .claim("full_name", user.getFullName())
            .claim("permissions", permissions)
            .claim("roles", roles)
            .claim("type", "ACCESS")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    /** Generate access token for SYSTEM_ADMIN */
    public String generateSystemToken(SystemUser user) {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(user.getUsername())
            .claim("user_id", user.getId())
            .claim("tenant_id", "SYSTEM")
            .claim("full_name", user.getFullName())
            .claim("permissions", List.of("*"))
            .claim("type", "SYSTEM_ACCESS")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    /** Generate refresh token */
    public String generateRefreshToken(String username, String tenantId) {
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(username)
            .claim("tenant_id", tenantId)
            .claim("type", "REFRESH")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    /** Parse and validate JWT, returns Claims */
    public Claims validateAndParse(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public String extractJti(String token)      { return validateAndParse(token).getId(); }
    public String extractUsername(String token)  { return validateAndParse(token).getSubject(); }
    public String extractTenantId(String token)  { return validateAndParse(token).get("tenant_id", String.class); }
    public Date   extractExpiration(String token) { return validateAndParse(token).getExpiration(); }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return validateAndParse(token).get("permissions", List.class);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public long getExpirationMs() { return jwtExpirationMs; }
}
