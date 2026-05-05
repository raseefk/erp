package com.supererp.erp.controller.api;

import com.supererp.erp.entity.AppUser;
import com.supererp.erp.entity.SystemUser;
import com.supererp.erp.entity.TokenBlacklist;
import com.supererp.erp.rbac.service.PermissionManifestBuilder;
import com.supererp.erp.repository.SystemUserRepository;
import com.supererp.erp.repository.TokenBlacklistRepository;
import com.supererp.erp.security.CustomUserDetailsService;
import com.supererp.erp.security.jwt.JwtAuthToken;
import com.supererp.erp.security.jwt.JwtTokenProvider;
import com.supererp.erp.tenant.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final CustomUserDetailsService  userDetailsService;
    private final SystemUserRepository      systemUserRepo;
    private final JwtTokenProvider          jwtProvider;
    private final PasswordEncoder           passwordEncoder;
    private final PermissionManifestBuilder manifestBuilder;
    private final TokenBlacklistRepository  blacklistRepo;

    // ── Tenant User Login ─────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Tenant could not be resolved. Check subdomain or X-Tenant-ID header."));
        }

        AppUser user = userDetailsService.loadAppUser(req.getUsername(), tenantId);
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled."));
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt: user={}, tenant={}", req.getUsername(), tenantId);
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken  = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername(), tenantId.toString());
        Map<String, Object> manifest = manifestBuilder.buildManifest(user);

        log.info("User logged in: {} (tenant: {})", user.getUsername(), tenantId);
        return ResponseEntity.ok(Map.of(
            "accessToken",  accessToken,
            "refreshToken", refreshToken,
            "expiresIn",    jwtProvider.getExpirationMs() / 1000,
            "manifest",     manifest
        ));
    }

    // ── System Admin Login ────────────────────────────────────────────────────
    @PostMapping("/system/login")
    public ResponseEntity<?> systemLogin(@RequestBody @Valid LoginRequest req) {
        SystemUser sysUser = systemUserRepo.findByUsernameAndEnabledTrue(req.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), sysUser.getPassword())) {
            log.warn("Failed SYSTEM_ADMIN login: {}", req.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtProvider.generateSystemToken(sysUser);
        log.info("SYSTEM_ADMIN logged in: {}", sysUser.getUsername());
        return ResponseEntity.ok(Map.of(
            "accessToken", token,
            "role",        "SYSTEM_ADMIN",
            "expiresIn",   jwtProvider.getExpirationMs() / 1000
        ));
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuth) {
            String rawToken = (String) jwtAuth.getCredentials();
            try {
                String jti = jwtProvider.extractJti(rawToken);
                blacklistRepo.save(TokenBlacklist.builder()
                    .jti(jti)
                    .reason("USER_LOGOUT")
                    .expiresAt(OffsetDateTime.now().plusDays(1))
                    .build());
            } catch (Exception ignored) {}
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;
    }
}
