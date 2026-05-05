package com.supererp.erp.controller.api;

import com.supererp.erp.tenant.Tenant;
import com.supererp.erp.tenant.TenantRepository;
import com.supererp.erp.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoint — no authentication required.
 * Returns tenant branding metadata for the login page.
 *
 * GET /api/v1/tenant/metadata?origin=https://acme.erp.com
 */
@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantMetadataController {

    private final TenantService tenantService;

    @GetMapping("/metadata")
    public ResponseEntity<?> getMetadata(
            @RequestParam(required = false) String origin,
            @RequestHeader(value = "X-Tenant-ID", required = false) String headerSlug) {

        String slug = resolveSlug(origin, headerSlug);
        if (slug == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Cannot resolve tenant from origin or header"));
        }

        return tenantService.findBySlug(slug)
            .map(t -> ResponseEntity.ok(Map.of(
                "tenantSlug",    t.getSlug(),
                "businessName",  t.getName(),
                "logoUrl",       t.getLogoUrl() != null ? t.getLogoUrl() : "",
                "primaryColor",  t.getPrimaryColor(),
                "loginTitle",    "Sign in to " + t.getName(),
                "plan",          t.getPlan(),
                "active",        t.isActive()
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    private String resolveSlug(String origin, String headerSlug) {
        if (headerSlug != null && !headerSlug.isBlank()) {
            return headerSlug.trim().toLowerCase();
        }
        if (origin != null) {
            try {
                String host = new java.net.URI(origin).getHost();
                if (host != null && host.contains(".")) {
                    String sub = host.split("\\.")[0];
                    if (!sub.equals("www")) return sub.toLowerCase();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
