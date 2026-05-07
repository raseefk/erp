package com.supererp.erp.config;

import com.supererp.erp.security.jwt.JwtAuthFilter;
import com.supererp.erp.tenant.TenantResolutionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import com.supererp.erp.security.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter            jwtAuthFilter;
    private final TenantResolutionFilter   tenantResolutionFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider authProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    /**
     * Main filter chain — JWT stateless for all /admin/**, /api/**, /settings/**, /hr/**
     */
    @Bean
    public SecurityFilterChain mainFilterChain(HttpSecurity http, PasswordEncoder passwordEncoder) throws Exception {
        http
            .securityMatcher(new AntPathRequestMatcher("/**"))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authenticationProvider(authProvider(passwordEncoder))
            .addFilterBefore(tenantResolutionFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // ── Fully Public ──────────────────────────────────────────────────
                .requestMatchers(
                    "/",
                    "/login", "/login/**",
                    "/api/v1/auth/**",
                    "/api/v1/tenant/metadata",
                    "/api/enquiries/submit",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico"
                ).permitAll()
                // ── System Admin ──────────────────────────────────────────────────
                .requestMatchers("/system/login").permitAll()
                .requestMatchers("/system/**").hasRole("SYSTEM_ADMIN")
                // ── Authenticated (permission checks happen via @PreAuthorize) ────
                .anyRequest().authenticated()
            )
            // ── Thymeleaf form login (for browser-based sessions) ──────────────
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    boolean isSystemAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
                    if (isSystemAdmin) {
                        response.sendRedirect("/system/tenants");
                    } else {
                        response.sendRedirect("/admin/home");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    String referer = request.getHeader("Referer");
                    if (referer != null && referer.contains("/system/login")) {
                        response.sendRedirect("/system/login?error=true");
                    } else {
                        response.sendRedirect("/login?error=true");
                    }
                })
                .permitAll()
            )
            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler((request, response, authentication) -> {
                    boolean isSystem = false;
                    if (authentication != null) {
                        isSystem = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
                    } else {
                        // Fallback check based on referer if authentication is already gone
                        String referer = request.getHeader("Referer");
                        if (referer != null && referer.contains("/system/")) {
                            isSystem = true;
                        }
                    }
                    
                    if (isSystem) {
                        response.sendRedirect("/system/login?logout=true");
                    } else {
                        response.sendRedirect("/login?logout=true");
                    }
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "erp_token")
                .permitAll()
            )
            .csrf(c -> c.ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/**"),
                new AntPathRequestMatcher("/system/tenants/**")
            ))
            .headers(h -> h
                .frameOptions(f -> f.sameOrigin())
                .httpStrictTransportSecurity(s -> s
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self';"
                ))
            );

        return http.build();
    }
}
