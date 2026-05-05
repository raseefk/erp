package com.levanto.flooring.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.levanto.flooring.security.CustomUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authProvider())
            .authorizeHttpRequests(auth -> auth
                // ── Public ─────────────────────────────────────────────────
                .requestMatchers(
                    "/", "/index.html",
                    "/api/enquiries/submit",
                    "/css/**", "/js/**", "/images/**", "/static/**",
                    "/favicon.ico", "/login", "/login/**",
                    "/h2-console/**"
                ).permitAll()
                // ── Admin-only modules ─────────────────────────────────────
                .requestMatchers("/admin/employees/**").hasRole("ADMIN")
                .requestMatchers("/admin/expenses/**").hasRole("ADMIN")
                .requestMatchers("/admin/vendors/**").hasRole("ADMIN")
                .requestMatchers("/admin/finance/**").hasRole("ADMIN")
                .requestMatchers("/admin/salaries/**").hasRole("ADMIN")
                .requestMatchers("/admin/payments/**").hasAnyRole("ADMIN","EMPLOYEE")
                .requestMatchers("/admin/approval/count").hasAnyRole("ADMIN","EMPLOYEE")
                .requestMatchers("/admin/approval/**").hasRole("ADMIN")
                .requestMatchers("/admin/projects/**").hasAnyRole("ADMIN","EMPLOYEE")
                .requestMatchers("/admin/sitelogs/**").hasAnyRole("ADMIN","EMPLOYEE")
                .requestMatchers("/admin/jobcards/**").hasAnyRole("ADMIN","EMPLOYEE")
                .requestMatchers("/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/hr/**").hasRole("ADMIN")
                .requestMatchers("/settings/**").hasRole("ADMIN")
                // ── Shared (Admin + Employee) ──────────────────────────────
                .requestMatchers("/admin/**").hasAnyRole("ADMIN","EMPLOYEE")
                .anyRequest().authenticated()
            )
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/admin/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(l -> l
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(s -> s
                .maximumSessions(5)
                .expiredUrl("/login?expired=true")
            )
            .headers(h -> h
                .frameOptions(f -> f.sameOrigin())
                .httpStrictTransportSecurity(s -> s
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000) // 1 year
                )
                .contentSecurityPolicy(c -> c.policyDirectives(
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
                    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                    "img-src 'self' data: https:; " +
                    "connect-src 'self';"
                ))
            )
            .csrf(c -> c.ignoringRequestMatchers(
                new AntPathRequestMatcher("/h2-console/**"),
                new AntPathRequestMatcher("/api/enquiries/submit")
            ));

        return http.build();
    }
}
