package com.supererp.erp.controller.api;

import com.supererp.erp.config.AppTenantConfig;
import com.supererp.erp.entity.AppUser;
import com.supererp.erp.entity.TokenBlacklist;
import com.supererp.erp.rbac.service.PermissionManifestBuilder;
import com.supererp.erp.repository.TokenBlacklistRepository;
import com.supererp.erp.security.CustomUserDetailsService;
import com.supererp.erp.security.jwt.JwtAuthToken;
import com.supererp.erp.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Authentication REST API — single-tenant mode.
 * Single login endpoint for all users; admin is just a user with the ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthApiController {

    private final CustomUserDetailsService  userDetailsService;
    private final JwtTokenProvider          jwtProvider;
    private final PasswordEncoder           passwordEncoder;
    private final PermissionManifestBuilder manifestBuilder;
    private final TokenBlacklistRepository  blacklistRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        AppUser user = userDetailsService.loadAppUser(req.getUsername());

        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(Map.of("error", "Account is disabled."));
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt: user={}", req.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken  = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(
            user.getUsername(), AppTenantConfig.APP_TENANT_ID.toString());
        Map<String, Object> manifest = manifestBuilder.buildManifest(user);

        log.info("User logged in: {}", user.getUsername());
        return ResponseEntity.ok(Map.of(
            "accessToken",  accessToken,
            "refreshToken", refreshToken,
            "expiresIn",    jwtProvider.getExpirationMs() / 1000,
            "manifest",     manifest
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthToken jwtAuth) {
            String rawToken = (String) jwtAuth.getCredentials();
            try {
                String jti = jwtProvider.extractJti(rawToken);
                java.util.Date expDate = jwtProvider.extractExpiration(rawToken);
                OffsetDateTime expiresAt = OffsetDateTime.ofInstant(
                    expDate.toInstant(), java.time.ZoneId.systemDefault());
                blacklistRepo.save(TokenBlacklist.builder()
                    .jti(jti)
                    .reason("USER_LOGOUT")
                    .expiresAt(expiresAt)
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
