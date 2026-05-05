package com.supererp.erp.controller;
import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.dto.EnquiryRequest;
import com.supererp.erp.service.EnquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Controller @RequiredArgsConstructor
public class PublicController {
    private final EnquiryService enquiryService;
    private final com.supererp.erp.tenant.TenantService tenantService;

    @GetMapping("/") public String index() { return "index"; }

    @GetMapping("/api/v1/tenant/metadata")
    @ResponseBody
    public ResponseEntity<?> getTenantMetadata(@RequestParam(required = false) String slug) {
        String targetSlug = (slug != null) ? slug : com.supererp.erp.tenant.TenantContext.getTenantSlug();
        
        if (targetSlug == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Slug must be provided or resolvable via host."));
        }

        return tenantService.findBySlug(targetSlug)
            .map(t -> ResponseEntity.ok(Map.of(
                "name",         t.getName(),
                "logoUrl",      t.getLogoUrl() != null ? t.getLogoUrl() : "",
                "primaryColor", t.getPrimaryColor() != null ? t.getPrimaryColor() : "#0f172a"
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/enquiries/submit") @ResponseBody
    public ResponseEntity<ApiResponse<?>> submit(@Valid @RequestBody EnquiryRequest req) {
        enquiryService.submit(req);
        return ResponseEntity.ok(ApiResponse.ok("Thank you! We will contact you shortly."));
    }
}
