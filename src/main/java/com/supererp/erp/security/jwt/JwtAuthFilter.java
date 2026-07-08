package com.supererp.erp.security.jwt;

import com.supererp.erp.entity.TokenBlacklist;
import com.supererp.erp.repository.TokenBlacklistRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT authentication filter — single-tenant mode.
 * Validates JWT signature, checks blacklist, and sets SecurityContext.
 * All tenant-mismatch and RLS logic has been removed.
 */
@Component
@Order(-90)
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider         jwtTokenProvider;
    private final TokenBlacklistRepository blacklistRepo;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
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
            String jti      = claims.getId();
            String username = claims.getSubject();
            Long   userId   = claims.get("user_id", Long.class);

            // ── 1. Token blacklist check ──────────────────────────────────
            if (blacklistRepo.existsByJti(jti)) {
                log.warn("Blacklisted token used: jti={}, user={}", jti, username);
                sendForbidden(response, "Token has been revoked");
                return;
            }

            // ── 2. Build authorities from claims ──────────────────────────
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (permissions != null) {
                permissions.stream()
                    .filter(p -> !"*".equals(p))
                    .forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));
            }
            if (roles != null) {
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
            }

            Boolean isSystem = claims.get("isSystem", Boolean.class);
            if (Boolean.TRUE.equals(isSystem)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }

            String tenantIdClaim = claims.get("tenant_id", String.class);
            JwtAuthToken auth = new JwtAuthToken(username, userId,
                tenantIdClaim, authorities, token, Boolean.TRUE.equals(isSystem));
            SecurityContextHolder.getContext().setAuthentication(auth);

            chain.doFilter(request, response);

        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("erp_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
