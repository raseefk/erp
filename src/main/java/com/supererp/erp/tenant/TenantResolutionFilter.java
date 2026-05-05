package com.supererp.erp.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            if (slug != null) {
                tenantService.findBySlug(slug).ifPresentOrElse(
                    tenant -> {
                        TenantContext.setTenantId(tenant.getId());
                        TenantContext.setTenantSlug(tenant.getSlug());
                        log.debug("Tenant resolved: {} ({})", slug, tenant.getId());
                    },
                    () -> log.warn("Unknown or inactive tenant slug: {}", slug)
                );
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private String resolveSlug(HttpServletRequest request) {
        // 1. Try subdomain
        String host = request.getServerName(); // e.g., acme.erp.com
        if (host != null && host.endsWith("." + tenantDomain)) {
            String subdomain = host.substring(0, host.length() - tenantDomain.length() - 1);
            if (!subdomain.isEmpty() && !subdomain.equals("www")) {
                return subdomain.toLowerCase();
            }
        }
        // 2. Fallback: X-Tenant-ID header (dev/internal)
        String headerVal = request.getHeader(headerFallback);
        if (headerVal != null && !headerVal.isBlank()) {
            return headerVal.trim().toLowerCase();
        }
        return null;
    }
}
