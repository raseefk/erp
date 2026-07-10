package com.supererp.erp.config;

import com.supererp.erp.entity.*;
import com.supererp.erp.rbac.entity.AppRole;
import com.supererp.erp.rbac.entity.TenantFeatureMapping;
import com.supererp.erp.rbac.repository.AppRoleRepository;
import com.supererp.erp.rbac.repository.TenantFeatureMappingRepository;
import com.supererp.erp.repository.*;
import com.supererp.erp.tenant.Tenant;
import com.supererp.erp.tenant.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Runs at startup (Order 2) to seed:
 * - Single application Tenant record
 * - System roles (ADMIN, EMPLOYEE)
 * - Default admin user
 * - All features enabled at application level
 * - Optional demo data (disabled by default)
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository              tenantRepo;
    private final AppUserRepository             userRepo;
    private final AppRoleRepository             roleRepo;
    private final TenantFeatureMappingRepository featureMapRepo;
    private final InventoryItemRepository       itemRepo;
    private final CustomerRepository            customerRepo;
    private final EmployeeRepository            employeeRepo;
    private final VendorRepository              vendorRepo;
    private final PasswordEncoder               encoder;
    private final EntityManager                 entityManager;

    @Value("${app.admin.username:admin}")
    private String adminUsername;
    @Value("${app.admin.password:Admin@1234}")
    private String adminPassword;
    @Value("${app.admin.email:admin@supererp.com}")
    private String adminEmail;
    @Value("${app.company.name:Super ERP}")
    private String companyName;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedApplicationTenant();
            seedRoles();
            seedAdminUser();
            seedAllFeatures();
            log.info("✅ DataInitializer complete");
        } catch (Exception e) {
            log.warn("⚠️ DataInitializer encountered an error (tables may not exist yet): {}", e.getMessage());
            log.debug("Full error:", e);
        }
    }

    private void seedApplicationTenant() {
        try {
            if (tenantRepo.count() > 0) {
                log.info("✅ Application tenant already exists");
                return;
            }
            
            Tenant app = Tenant.builder()
                .id(AppTenantConfig.APP_TENANT_ID)
                .slug("app")
                .name(companyName)
                .primaryColor("#3b82f6")
                .plan("ENTERPRISE")
                .maxUsers(10000)
                .maxStorageGb(1000.0)
                .active(true)
                .build();
            tenantRepo.saveAndFlush(app);
            log.info("✅ Application tenant seeded — id: {}", AppTenantConfig.APP_TENANT_ID);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.info("✅ Application tenant already exists (constraint violation - data already present)");
        } catch (Exception e) {
            log.warn("⚠️ Could not seed tenant: {}", e.getMessage());
        }
    }

    private void seedRoles() {
        try {
            if (!roleRepo.existsByName("SUPER_ADMIN")) {
                roleRepo.save(AppRole.builder()
                    .tenantId(AppTenantConfig.APP_TENANT_ID)
                    .name("SUPER_ADMIN")
                    .description("Super Administrator with application administration access")
                    .system(true)
                    .build());
                log.info("✅ SUPER_ADMIN role created");
            }
            if (!roleRepo.existsByName("ADMIN")) {
                roleRepo.save(AppRole.builder()
                    .tenantId(AppTenantConfig.APP_TENANT_ID)
                    .name("ADMIN")
                    .description("Full application administrator")
                    .system(true)
                    .build());
                log.info("✅ ADMIN role created");
            }
            if (!roleRepo.existsByName("EMPLOYEE")) {
                roleRepo.save(AppRole.builder()
                    .tenantId(AppTenantConfig.APP_TENANT_ID)
                    .name("EMPLOYEE")
                    .description("Standard employee access")
                    .system(false)
                    .build());
                log.info("✅ EMPLOYEE role created");
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not seed roles (table may not be ready): {}", e.getMessage());
        }
    }

    private void seedAdminUser() {
        AppRole adminRole = roleRepo.findByName("ADMIN")
            .orElseThrow(() -> new IllegalStateException("ADMIN role not found — seedRoles() must run first"));
            
        AppRole superAdminRole = roleRepo.findByName("SUPER_ADMIN")
            .orElseThrow(() -> new IllegalStateException("SUPER_ADMIN role not found — seedRoles() must run first"));

        // Assign all permissions to both roles
        entityManager.createNativeQuery(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT :roleId, id FROM permissions " +
            "WHERE id NOT IN (SELECT permission_id FROM role_permissions WHERE role_id = :roleId)")
            .setParameter("roleId", adminRole.getId())
            .executeUpdate();

        entityManager.createNativeQuery(
            "INSERT INTO role_permissions (role_id, permission_id) " +
            "SELECT :roleId, id FROM permissions " +
            "WHERE id NOT IN (SELECT permission_id FROM role_permissions WHERE role_id = :roleId)")
            .setParameter("roleId", superAdminRole.getId())
            .executeUpdate();

        if (userRepo.existsByUsername(adminUsername)) {
            // Sync password and roles on every startup
            userRepo.findByUsername(adminUsername).ifPresent(u -> {
                u.setPassword(encoder.encode(adminPassword));
                u.setEnabled(true);
                if (u.getRoles() == null) u.setRoles(new HashSet<>());
                u.getRoles().add(adminRole);
                u.getRoles().add(superAdminRole);
                userRepo.save(u);
            });
            log.info("✅ Admin user '{}' synchronized with SUPER_ADMIN role", adminUsername);
            return;
        }

        AppUser admin = AppUser.builder()
            .tenantId(AppTenantConfig.APP_TENANT_ID)
            .username(adminUsername)
            .password(encoder.encode(adminPassword))
            .fullName("Administrator")
            .email(adminEmail)
            .enabled(true)
            .roles(new HashSet<>(Set.of(adminRole, superAdminRole)))
            .build();
        userRepo.save(admin);
        log.info("✅ Admin user '{}' created with SUPER_ADMIN role", adminUsername);
    }

    private void seedAllFeatures() {
        List<String> features = List.of(
            "SALES", "OPERATIONS", "SCM", "PROJECTS", "ASSETS",
            "CONSTRUCTION", "HR", "FINANCE", "ADMIN", "ADVANCE_PAYMENTS",
            "DMS", "WMS", "SYSTEM"
        );
        for (String f : features) {
            var key = new com.supererp.erp.rbac.entity.TenantFeatureId(AppTenantConfig.APP_TENANT_ID, f);
            if (!featureMapRepo.existsById(key)) {
                featureMapRepo.save(TenantFeatureMapping.builder()
                    .tenantId(AppTenantConfig.APP_TENANT_ID)
                    .featureId(f)
                    .enabled(true)
                    .build());
            }
        }
        log.info("✅ All features enabled at application level");
    }
}
