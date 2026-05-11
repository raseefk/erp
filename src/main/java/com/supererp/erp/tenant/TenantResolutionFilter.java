package com.supererp.erp.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.supererp.erp.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Priority -100 filter that resolves the tenant from the incoming request.
 * Strategy: Subdomain first (e.g., acme.erp.com → slug "acme"),
 *           fallback to X-Tenant-ID header for local development.
 *
 * Public paths (/api/v1/tenant/metadata, /system/**) bypass tenant resolution.
 */
@Component
@Order(-100)
@RequiredArgsConstructor
@Slf4j
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantService tenantService;
    private final org.springframework.core.env.Environment env;

    @Value("${app.tenant.domain:erp.com}")
    private String tenantDomain;

    @Value("${app.tenant.header-fallback:X-Tenant-ID}")
    private String headerFallback;

    private static final String[] BYPASS_PATHS = {
        "/api/v1/tenant/metadata",
        "/system/",
        "/actuator/",
        "/favicon.ico",
        "/css/", "/js/", "/images/", "/static/",
        "/api/v1/auth/"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String bp : BYPASS_PATHS) {
            if (path.startsWith(bp)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String slug = resolveSlug(request);
            log.debug("TenantResolutionFilter: Resolved slug '{}' from request {}", slug, request.getRequestURI());
            if (slug != null) {
                tenantService.findBySlug(slug).ifPresentOrElse(
                    tenant -> {
                        TenantContext.setTenantId(tenant.getId());
                        TenantContext.setTenantSlug(tenant.getSlug());
                        request.setAttribute("currentTenant", tenant);
                        log.debug("TenantResolutionFilter: Tenant '{}' resolved to ID {}", slug, tenant.getId());
                    },
                    () -> log.warn("TenantResolutionFilter: Unknown or inactive tenant slug: {}", slug)
                );
            } else {
                // Fallback: Resolve from SecurityContext if authenticated (useful for localhost/dev)
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (auth != null) {
                    if (auth.getPrincipal() instanceof SecurityUser) {
                        SecurityUser user = (SecurityUser) auth.getPrincipal();
                        if (user.getTenantId() != null) {
                            TenantContext.setTenantId(user.getTenantId());
                            log.debug("Tenant resolved from SecurityContext (SecurityUser): {}", user.getTenantId());
                        }
                    } else if (auth instanceof com.supererp.erp.security.jwt.JwtAuthToken) {
                        String tid = ((com.supererp.erp.security.jwt.JwtAuthToken) auth).getTenantId();
                        if (tid != null && !"SYSTEM".equals(tid)) {
                            UUID uuid = UUID.fromString(tid);
                            TenantContext.setTenantId(uuid);
                            log.debug("Tenant resolved from SecurityContext (JwtAuthToken): {}", uuid);
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveSlug(HttpServletRequest request) {
        // 1. Try subdomain
        String host = request.getServerName(); // e.g., acme.erp.com or acme.localhost
        if (host != null) {
            if (host.endsWith("." + tenantDomain)) {
                String subdomain = host.substring(0, host.length() - tenantDomain.length() - 1);
                if (!subdomain.isEmpty() && !subdomain.equals("www")) {
                    return subdomain.toLowerCase();
                }
            } else if (host.contains(".") && (host.endsWith(".localhost") || host.contains(".127.0.0.1"))) {
                // Local development support (e.g., acme.localhost)
                String subdomain = host.substring(0, host.indexOf("."));
                if (!subdomain.isEmpty() && !subdomain.equals("www")) {
                    return subdomain.toLowerCase();
                }
            }
        }
        // 2. Fallback: X-Tenant-ID header (dev/internal)
        boolean isProd = false;
        for (String profile : env.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                isProd = true;
                break;
            }
        }
        if (!isProd) {
            String headerVal = request.getHeader(headerFallback);
            if (headerVal != null && !headerVal.isBlank()) {
                return headerVal.trim().toLowerCase();
            }
        }
        return null;
    }
}
