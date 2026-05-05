package com.supererp.erp.config;

import com.supererp.erp.entity.*;
import com.supererp.erp.rbac.entity.AppRole;
import com.supererp.erp.rbac.entity.TenantFeatureMapping;
import com.supererp.erp.rbac.repository.AppRoleRepository;
import com.supererp.erp.rbac.repository.TenantFeatureMappingRepository;
import com.supererp.erp.repository.*;
import com.supererp.erp.tenant.Tenant;
import com.supererp.erp.tenant.TenantRepository;
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
 *  - SYSTEM_ADMIN superuser
 *  - Default "demo" tenant
 *  - System roles (ADMIN, EMPLOYEE) for the demo tenant
 *  - Demo data (inventory, customers, employees, vendors)
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SystemUserRepository          systemUserRepo;
    private final TenantRepository              tenantRepo;
    private final AppUserRepository             userRepo;
    private final AppRoleRepository             roleRepo;
    private final TenantFeatureMappingRepository featureMapRepo;
    private final InventoryItemRepository       itemRepo;
    private final CustomerRepository            customerRepo;
    private final EmployeeRepository            employeeRepo;
    private final VendorRepository              vendorRepo;
    private final PasswordEncoder               encoder;

    @Value("${app.system.admin.username:superadmin}")
    private String systemAdminUsername;
    @Value("${app.system.admin.password:SuperAdmin@2026!}")
    private String systemAdminPassword;
    @Value("${app.system.admin.email:admin@supererp.com}")
    private String systemAdminEmail;

    @Value("${app.admin.username:admin}")
    private String tenantAdminUsername;
    @Value("${app.admin.password:Admin@1234}")
    private String tenantAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedSystemAdmin();
        UUID demoTenantId = seedDemoTenant();
        if (demoTenantId != null) {
            seedRoles(demoTenantId);
            seedTenantAdmin(demoTenantId);
            seedAllFeatures(demoTenantId);
            seedInventory(demoTenantId);
            seedCustomers(demoTenantId);
            seedEmployees(demoTenantId);
            seedVendors(demoTenantId);
        }
    }

    private void seedSystemAdmin() {
        if (!systemUserRepo.existsByUsername(systemAdminUsername)) {
            systemUserRepo.save(SystemUser.builder()
                .username(systemAdminUsername)
                .password(encoder.encode(systemAdminPassword))
                .fullName("Super Administrator")
                .email(systemAdminEmail)
                .enabled(true).build());
            log.info("✅ SYSTEM_ADMIN created — login: '{}' / '{}'",
                systemAdminUsername, systemAdminPassword);
        }
    }

    private UUID seedDemoTenant() {
        if (tenantRepo.findBySlug("demo").isPresent()) {
            return tenantRepo.findBySlug("demo").get().getId();
        }
        Tenant demo = tenantRepo.save(Tenant.builder()
            .slug("demo")
            .name("Demo Company")
            .primaryColor("#3b82f6")
            .plan("STANDARD")
            .maxUsers(10)
            .active(true).build());
        log.info("✅ Demo tenant created — slug: 'demo'");
        return demo.getId();
    }

    private void seedRoles(UUID tenantId) {
        if (!roleRepo.existsByTenantIdAndName(tenantId, "ADMIN")) {
            roleRepo.save(AppRole.builder()
                .tenantId(tenantId).name("ADMIN")
                .description("Full access administrator").system(true).build());
        }
        if (!roleRepo.existsByTenantIdAndName(tenantId, "EMPLOYEE")) {
            roleRepo.save(AppRole.builder()
                .tenantId(tenantId).name("EMPLOYEE")
                .description("Standard employee access").system(true).build());
        }
    }

    private void seedTenantAdmin(UUID tenantId) {
        if (!userRepo.existsByUsernameAndTenantId(tenantAdminUsername, tenantId)) {
            AppRole adminRole = roleRepo.findByTenantIdAndName(tenantId, "ADMIN")
                .orElseThrow();
            AppUser admin = userRepo.save(AppUser.builder()
                .tenantId(tenantId)
                .username(tenantAdminUsername)
                .password(encoder.encode(tenantAdminPassword))
                .fullName("Administrator")
                .enabled(true).build());
            admin.getRoles().add(adminRole);
            userRepo.save(admin);
            log.info("✅ Tenant admin created — '{}' / '{}'", tenantAdminUsername, tenantAdminPassword);
        }
    }

    private void seedAllFeatures(UUID tenantId) {
        List<String> features = List.of("BILLING","CRM","INVENTORY","SCM","PROJECTS","HR","FINANCE","SETTINGS");
        for (String f : features) {
            if (!featureMapRepo.existsById(new com.supererp.erp.rbac.entity.TenantFeatureId(tenantId, f))) {
                featureMapRepo.save(TenantFeatureMapping.builder()
                    .tenantId(tenantId).featureId(f).enabled(true).build());
            }
        }
    }

    private void seedInventory(UUID tenantId) {
        if (itemRepo.countByTenantId(tenantId) > 0) return;
        itemRepo.save(item(tenantId, "Vitrified Tiles 600x600mm", "PRODUCT",
            "Premium double-charged vitrified tiles", "55.00", 2000, "6908", "SQF"));
        itemRepo.save(item(tenantId, "Wooden Laminate Flooring 8mm", "PRODUCT",
            "AC4 rated click-lock laminate", "72.00", 1500, "4412", "SQF"));
        itemRepo.save(item(tenantId, "Luxury Vinyl Plank (SPC 5mm)", "PRODUCT",
            "100% waterproof SPC core vinyl plank", "85.00", 1200, "3918", "SQF"));
        itemRepo.save(item(tenantId, "Flooring Installation", "SERVICE",
            "Professional tile laying and fixing", "18.00", 0, "995466", "SQF"));
        itemRepo.save(item(tenantId, "Waterproofing Treatment", "SERVICE",
            "Bathroom/terrace waterproofing", "35.00", 0, "995431", "SQF"));
        log.info("✅ Sample inventory seeded for demo tenant");
    }

    private void seedCustomers(UUID tenantId) {
        if (customerRepo.countByTenantId(tenantId) > 0) return;
        customerRepo.save(Customer.builder().tenantId(tenantId)
            .name("Demo Customer").phone("9876543210")
            .email("customer@demo.com").address("123 Main St, City").build());
        log.info("✅ Sample customers seeded for demo tenant");
    }

    private void seedEmployees(UUID tenantId) {
        if (employeeRepo.countByTenantId(tenantId) > 0) return;
        employeeRepo.save(Employee.builder().tenantId(tenantId)
            .name("John Doe").phone("9988776655")
            .designation("Site Engineer")
            .monthlySalary(new BigDecimal("22000"))
            .joiningDate(LocalDate.of(2024, 1, 1)).active(true).build());
        log.info("✅ Sample employees seeded for demo tenant");
    }

    private void seedVendors(UUID tenantId) {
        if (vendorRepo.countByTenantId(tenantId) > 0) return;
        vendorRepo.save(Vendor.builder().tenantId(tenantId)
            .name("Demo Supplier").contactPerson("Supplier Rep")
            .phone("9900112233").email("supplier@demo.com")
            .materialSupplied("Tiles, Adhesive").active(true).build());
        log.info("✅ Sample vendors seeded for demo tenant");
    }

    private InventoryItem item(UUID tenantId, String name, String type, String desc,
                                String price, int stock, String hsn, String unit) {
        return InventoryItem.builder()
            .tenantId(tenantId).name(name).itemType(com.supererp.erp.enums.ItemType.valueOf(type))
            .description(desc).currentPrice(new BigDecimal(price))
            .stockQuantity("PRODUCT".equals(type) ? stock : 0)
            .hsnSacCode(hsn).unit(unit).active(true).build();
    }
}
