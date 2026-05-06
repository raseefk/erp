package com.supererp.erp.rbac;

import com.supererp.erp.rbac.entity.*;
import com.supererp.erp.rbac.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Runs at startup (Order 1) to sync all permission constants from
 * Permissions.java into the database.
 * Also seeds Features and Menus if not present.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PermissionSeeder implements CommandLineRunner {

    private final FeatureRepository     featureRepo;
    private final MenuRepository        menuRepo;
    private final PermissionRepository  permRepo;

    @Override
    public void run(String... args) {
        seedFeatures();
        seedMenus();
        seedPermissions();
        log.info("✅ Permission/Feature/Menu schema seeded");
    }

    private void seedFeatures() {
        upsertFeature("SALES",      "Sales",               "receipt",     1);
        upsertFeature("OPERATIONS", "Operations",          "boxes",       2);
        upsertFeature("SCM",        "Supply Chain",        "truck",       3);
        upsertFeature("PROJECTS",   "Projects",            "folder-open", 4);
        upsertFeature("HR",         "HR",                  "id-card",     5);
        upsertFeature("FINANCE",    "Finance",             "coins",       6);
        upsertFeature("ADMIN",      "Administration",      "shield-lock", 7);
        upsertFeature("SYSTEM",     "System",              "cog",         8);
    }

    private void upsertFeature(String id, String name, String icon, int order) {
        if (!featureRepo.existsById(id)) {
            featureRepo.save(Feature.builder()
                .id(id).displayName(name).icon(icon).sortOrder(order).build());
        }
    }

    private void seedMenus() {
        // Sales
        upsertMenu("MENU_ENQUIRIES", "SALES", "Enquiries", "chat-left-text", 1);
        upsertMenu("MENU_BILLING",   "SALES", "Billing & Quotations", "receipt-cutoff", 2);
        upsertMenu("MENU_CUSTOMERS", "SALES", "Customers", "people", 3);

        // Operations
        upsertMenu("MENU_INVENTORY", "OPERATIONS", "Inventory", "boxes", 1);

        // Supply Chain
        upsertMenu("MENU_VENDORS", "SCM", "Vendor Directory", "truck", 1);
        upsertMenu("MENU_PURCHASE_ORDERS", "SCM", "Purchase Orders", "cart", 2);

        // Projects
        upsertMenu("MENU_PROJECTS", "PROJECTS", "Projects", "kanban", 1);
        upsertMenu("MENU_SITELOGS", "PROJECTS", "Daily Logs", "journal-text", 2);
        upsertMenu("MENU_APPROVALS", "PROJECTS", "Approval Queue", "check2-square", 3);

        // HR
        upsertMenu("MENU_EMPLOYEES", "HR", "Employees", "person-badge", 1);
        upsertMenu("MENU_SALARIES", "HR", "Salaries", "cash-stack", 2);
        upsertMenu("MENU_ATTENDANCE", "HR", "Daily Ledger", "clock-history", 3);
        upsertMenu("MENU_ATTENDANCE_REPORT", "HR", "Attendance Report", "calendar3", 4);
        upsertMenu("MENU_LEAVES", "HR", "Leaves", "calendar2-minus", 5);

        // Finance
        upsertMenu("MENU_EXPENSES", "FINANCE", "Expenses", "wallet2", 1);
        upsertMenu("MENU_PAYMENTS", "FINANCE", "Payments", "cash-coin", 2);
        upsertMenu("MENU_PL_REPORT", "FINANCE", "P&L Report", "graph-up-arrow", 3);

        // Administration
        upsertMenu("MENU_USERS", "ADMIN", "User Management", "shield-lock", 1);
        upsertMenu("MENU_ROLES", "ADMIN", "Roles & Permissions", "shield-check", 2);

        // System
        upsertMenu("MENU_SETTINGS", "SYSTEM", "Settings", "gear", 1);
        upsertMenu("MENU_HOLIDAYS", "SYSTEM", "Annual Holidays", "calendar-event", 2);
    }

    private void upsertMenu(String id, String featureId, String name, String icon, int order) {
        if (!menuRepo.existsById(id)) {
            Feature feat = featureRepo.findById(featureId).orElse(null);
            if (feat != null) {
                menuRepo.save(Menu.builder()
                    .id(id).feature(feat).displayName(name).icon(icon).sortOrder(order).build());
            }
        }
    }

    private void seedPermissions() {
        // Reflect over all public static String fields in Permissions.java
        Map<String, String> permDefs = extractPermissionDefs();
        int created = 0;
        for (Map.Entry<String, String> e : permDefs.entrySet()) {
            String id = e.getValue(); // e.g. "BILLING_INVOICES_VIEW"
            if (!permRepo.existsById(id)) {
                String featureId = resolveFeature(id);
                String action    = resolveAction(id);
                String display   = toDisplayName(id);
                Feature feature  = featureRepo.findById(featureId).orElse(null);
                if (feature != null) {
                    permRepo.save(Permission.builder()
                        .id(id)
                        .feature(feature)
                        .displayName(display)
                        .action(action)
                        .build());
                    created++;
                }
            }
        }
        if (created > 0) log.info("✅ Seeded {} new permissions", created);
    }

    private Map<String, String> extractPermissionDefs() {
        Map<String, String> defs = new LinkedHashMap<>();
        try {
            for (Field f : Permissions.class.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    defs.put(f.getName(), (String) f.get(null));
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Could not read Permissions class", e);
        }
        return defs;
    }

    private String resolveFeature(String permId) {
        // First segment of permission ID = feature
        return permId.split("_")[0];
    }

    private String resolveAction(String permId) {
        // Last segment = action
        String[] parts = permId.split("_");
        return parts[parts.length - 1];
    }

    private String toDisplayName(String permId) {
        return permId.replace("_", " ").toLowerCase();
    }
}
