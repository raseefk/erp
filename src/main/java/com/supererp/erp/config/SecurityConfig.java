package com.supererp.erp.config;

import com.supererp.erp.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import com.supererp.erp.security.CustomUserDetailsService;

/**
 * Security configuration — single-tenant mode.
 * - Single login page at /login (no separate system admin portal)
 * - Admin users are ordinary AppUsers with the ADMIN role
 * - JWT filter handles token auth for /api/** endpoints
 * - Form login handles browser sessions for the Thymeleaf UI
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter            jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;
    private final com.supererp.erp.security.jwt.JwtTokenProvider jwtTokenProvider;
    private final com.supererp.erp.repository.TokenBlacklistRepository blacklistRepo;

    @Bean
    public DaoAuthenticationProvider authProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public SecurityFilterChain mainFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        http
            .securityMatcher(new AntPathRequestMatcher("/**"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authenticationProvider(authProvider(passwordEncoder))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // ── Public ──────────────────────────────────────────────────
                .requestMatchers(
                    "/",
                    "/login", "/login/**",
                    "/api/v1/auth/**",
                    "/api/enquiries/submit",
                    "/api/public/erp-enquiries",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico",
                    "/actuator/health", "/actuator/info"
                ).permitAll()
                // ── Everything else requires authentication ─────────────────
                .anyRequest().authenticated()
            )
            // ── Form login (Thymeleaf UI) ───────────────────────────────────
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) ->
                    response.sendRedirect(request.getContextPath() + "/admin/home"))
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .addLogoutHandler((request, response, authentication) -> {
                    String token = null;
                    if (request.getCookies() != null) {
                        for (var cookie : request.getCookies()) {
                            if ("erp_token".equals(cookie.getName())) {
                                token = cookie.getValue();
                                break;
                            }
                        }
                    }
                    if (token != null) {
                        try {
                            String jti = jwtTokenProvider.extractJti(token);
                            java.util.Date expDate = jwtTokenProvider.extractExpiration(token);
                            java.time.OffsetDateTime expiresAt = java.time.OffsetDateTime
                                .ofInstant(expDate.toInstant(), java.time.ZoneId.systemDefault());
                            blacklistRepo.save(com.supererp.erp.entity.TokenBlacklist.builder()
                                .jti(jti)
                                .reason("USER_LOGOUT")
                                .expiresAt(expiresAt)
                                .build());
                        } catch (Exception ignored) {}
                    }
                })
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "erp_token", "SUPERERP_SESSION")
                .permitAll()
            )
            .csrf(c -> c.ignoringRequestMatchers(new AntPathRequestMatcher("/api/**")))
            .headers(h -> h
                .frameOptions(f -> f.sameOrigin())
                .httpStrictTransportSecurity(s -> s.includeSubDomains(true).maxAgeInSeconds(0))
                .contentTypeOptions(org.springframework.security.config.Customizer.withDefaults())
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self';"
                ))
            );

        return http.build();
    }
}
