package com.levanto.flooring.config;

import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.*;
import com.levanto.flooring.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import com.levanto.flooring.entity.Project;
import com.levanto.flooring.entity.JobCard;
import com.levanto.flooring.enums.ProjectStatus;
import com.levanto.flooring.enums.JobCardStatus;
import com.levanto.flooring.repository.ProjectRepository;
import com.levanto.flooring.repository.JobCardRepository;
import java.time.LocalDate;

@Component @RequiredArgsConstructor @Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository        userRepo;
    private final InventoryItemRepository  itemRepo;
    private final CustomerRepository       customerRepo;
    private final EmployeeRepository       employeeRepo;
    private final VendorRepository         vendorRepo;
    private final PasswordEncoder          encoder;
    private final ProjectRepository        projectRepo;
    private final JobCardRepository        jobCardRepo;

    @Value("${app.admin.username:admin}")
    private String adminUsername;
    @Value("${app.admin.password:Admin@1234}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedInventory();
        seedCustomers();
        seedEmployees();
        seedVendors();
        seedSampleProject();
    }

    private void seedAdmin() {
        if (!userRepo.existsByUsername(adminUsername)) {
            userRepo.save(AppUser.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .fullName("Administrator")
                .role(Role.ROLE_ADMIN)
                .enabled(true).build());
            log.info("✅ Admin user created — login: '{}' / '{}'", adminUsername, adminPassword);
        }
    }

    private void seedInventory() {
        if (itemRepo.count() > 0) return;
        // Products
        itemRepo.save(item("Vitrified Tiles 600x600mm", ItemType.PRODUCT,
            "Premium double-charged vitrified tiles", "55.00", 2000, "6908", "SQF"));
        itemRepo.save(item("Wooden Laminate Flooring 8mm", ItemType.PRODUCT,
            "AC4 rated click-lock laminate", "72.00", 1500, "4412", "SQF"));
        itemRepo.save(item("Luxury Vinyl Plank (SPC 5mm)", ItemType.PRODUCT,
            "100% waterproof SPC core vinyl plank", "85.00", 1200, "3918", "SQF"));
        itemRepo.save(item("Ceramic Wall Tiles 300x600mm", ItemType.PRODUCT,
            "High gloss glazed ceramic", "38.00", 3000, "6907", "SQF"));
        itemRepo.save(item("Anti-Skid Outdoor Tiles", ItemType.PRODUCT,
            "Matt finish porcelain for balcony/outdoor", "48.00", 800, "6908", "SQF"));
        itemRepo.save(item("Tile Adhesive (20kg bag)", ItemType.PRODUCT,
            "C1 grade polymer tile adhesive", "420.00", 500, "3214", "BAG"));
        itemRepo.save(item("Tile Grout (1kg)", ItemType.PRODUCT,
            "Unsanded cementitious grout", "85.00", 300, "3214", "KG"));
        // Services
        itemRepo.save(item("Flooring Installation", ItemType.SERVICE,
            "Professional tile laying and fixing", "18.00", 0, "995466", "SQF"));
        itemRepo.save(item("Skirting / Border Work", ItemType.SERVICE,
            "Ceramic skirting tile installation", "25.00", 0, "995466", "RFT"));
        itemRepo.save(item("Waterproofing Treatment", ItemType.SERVICE,
            "Bathroom/terrace waterproofing", "35.00", 0, "995431", "SQF"));
        itemRepo.save(item("Old Floor Removal", ItemType.SERVICE,
            "Breaking and disposing old flooring", "12.00", 0, "995466", "SQF"));
        itemRepo.save(item("Floor Polishing", ItemType.SERVICE,
            "Machine polishing of marble/granite", "8.00", 0, "995466", "SQF"));
        log.info("✅ Sample inventory seeded (7 products + 5 services)");
    }

    private void seedCustomers() {
        if (customerRepo.count() > 0) return;
        customerRepo.save(Customer.builder().name("Caokon City").phone("9876543210")
            .email("rahul@example.com").address("12, MG Road, Calicut, Kerala - 680001").build());
        customerRepo.save(Customer.builder().name("Salman K").phone("9845123456")
            .email("priya@example.com").address("45, Kakkadampoyil, Calicut, Kerala - 678001")
            .gstNumber("32ABCDE1234F1Z5").build());
        log.info("✅ Sample customers seeded");
    }

    private void seedEmployees() {
        if (employeeRepo.count() > 0) return;
        employeeRepo.save(Employee.builder().name("Akbar V").phone("9988776655")
            .designation("Sales Executive").monthlySalary(new BigDecimal("22000"))
            .joiningDate(LocalDate.of(2022, 1, 15)).active(true).build());
        employeeRepo.save(Employee.builder().name("Shyam PK").phone("9871234560")
            .designation("Site Engineer").monthlySalary(new BigDecimal("18000"))
            .joiningDate(LocalDate.of(2023, 6, 1)).active(true).build());
        log.info("✅ Sample employees seeded");
    }

    private void seedVendors() {
        if (vendorRepo.count() > 0) return;
        vendorRepo.save(Vendor.builder().name("Kerala Tiles Depot").contactPerson("Suresh")
            .phone("9900112233").email("kerala.tiles@example.com")
            .address("Industrial Area, Thrissur").materialSupplied("Vitrified & Ceramic Tiles")
            .gstNumber("32AAAPL1234C1ZK").active(true).build());
        vendorRepo.save(Vendor.builder().name("BuildMart Adhesives").contactPerson("Rajan")
            .phone("9811223344").email("buildmart@example.com")
            .address("Coimbatore, Tamil Nadu").materialSupplied("Adhesive, Grout, Chemicals")
            .active(true).build());
        log.info("✅ Sample vendors seeded");
    }

    private InventoryItem item(String name, ItemType type, String desc,
                               String price, int stock, String hsn, String unit) {
        return InventoryItem.builder()
            .name(name).itemType(type).description(desc)
            .currentPrice(new BigDecimal(price))
            .stockQuantity(type == ItemType.PRODUCT ? stock : 0)
            .hsnSacCode(hsn).unit(unit).active(true).build();
    }

    private void seedSampleProject() {
        if (projectRepo.count() > 0) return;
        Project p = Project.builder()
            .name("Capkon City — Lobby Flooring")
            .clientName("Capkon Builders")
            .location("Pantheeramkavu, Calicut")
            .totalContractValue(new java.math.BigDecimal("450000"))
            .startDate(LocalDate.now().minusDays(10))
            .endDate(LocalDate.now().plusDays(50))
            .status(ProjectStatus.ACTIVE)
            .description("Premium vitrified tile flooring for lobby and corridors.")
            .build();
        p = projectRepo.save(p);

        JobCard jc1 = JobCard.builder()
            .project(p).phase("Tile Laying — Ground Floor")
            .description("600x600 vitrified tiles, lobby and corridors.")
            .status(JobCardStatus.IN_PROGRESS)
            .targetDate(LocalDate.now().plusDays(20))
            .build();
        jobCardRepo.save(jc1);

        JobCard jc2 = JobCard.builder()
            .project(p).phase("Skirting & Border Work")
            .description("Ceramic skirting tiles along all walls.")
            .status(JobCardStatus.PLANNED)
            .targetDate(LocalDate.now().plusDays(35))
            .build();
        jobCardRepo.save(jc2);

        log.info("✅ Sample project + 2 job cards seeded");
    }

}