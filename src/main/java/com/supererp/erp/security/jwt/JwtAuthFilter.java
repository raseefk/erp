package com.supererp.erp.security.jwt;

import com.supererp.erp.entity.TokenBlacklist;
import com.supererp.erp.repository.TokenBlacklistRepository;
import com.supererp.erp.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JWT authentication filter — Priority -90 (runs after TenantResolutionFilter).
 *
 * Security checks:
 *  1. Extract Bearer token
 *  2. Validate signature + expiry
 *  3. Check token blacklist
 *  4. Compare JWT tenant_id with TenantContext (set by TenantResolutionFilter)
 *     → MISMATCH = Security Breach: blacklist token + 403
 *  5. Set SecurityContext
 *  6. Set PostgreSQL session variables (RLS)
 */
@Component
@Order(-90)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final TokenBlacklistRepository blacklistRepo;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/system/login")
            || path.startsWith("/api/v1/tenant/metadata")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);
        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.validateAndParse(token);
            String jti          = claims.getId();
            String jwtTenantId  = claims.get("tenant_id", String.class);
            String username     = claims.getSubject();
            Long   userId       = claims.get("user_id", Long.class);

            // ── 1. Token blacklist check ──────────────────────────────────
            if (isBlacklisted(jti)) {
                log.warn("Blacklisted token used: jti={}, user={}", jti, username);
                sendForbidden(response, "Token has been revoked");
                return;
            }

            // ── 2. Tenant mismatch = SECURITY BREACH ──────────────────────
            if (!"SYSTEM".equals(jwtTenantId) && TenantContext.hasActiveTenant()) {
                UUID contextTenant = TenantContext.getTenantId();
                if (!jwtTenantId.equals(contextTenant.toString())) {
                    triggerSecurityBreach(jti, username, jwtTenantId,
                        contextTenant.toString(), request, response);
                    return;
                }
            }

            // ── 3. Set SecurityContext ─────────────────────────────────────
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            List<SimpleGrantedAuthority> authorities = (permissions != null ? permissions : List.<String>of())
                .stream()
                .map(p -> new SimpleGrantedAuthority("PERM_" + p))
                .collect(Collectors.toList());

            // Add SYSTEM_ADMIN role if applicable
            if ("SYSTEM".equals(jwtTenantId)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"));
            }

            JwtAuthToken auth = new JwtAuthToken(username, userId,
                jwtTenantId, authorities, token);
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);

        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Authorization header
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // 2. Cookie (for Thymeleaf-based auth)
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("erp_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isBlacklisted(String jti) {
        return blacklistRepo.existsByJti(jti);
    }

    private void triggerSecurityBreach(String jti, String username,
                                        String jwtTenantId, String contextTenant,
                                        HttpServletRequest request,
                                        HttpServletResponse response) throws IOException {
        log.error("🚨 SECURITY BREACH DETECTED — user={}, jwtTenant={}, contextTenant={}, ip={}",
            username, jwtTenantId, contextTenant, request.getRemoteAddr());

        // Blacklist the token immediately
        TokenBlacklist blacklisted = TokenBlacklist.builder()
            .jti(jti)
            .reason("SECURITY_BREACH_TENANT_MISMATCH")
            .expiresAt(OffsetDateTime.now().plusDays(7))
            .build();
        blacklistRepo.save(blacklisted);

        SecurityContextHolder.clearContext();
        sendForbidden(response,
            "Security violation detected. Session terminated. Please contact support.");
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
