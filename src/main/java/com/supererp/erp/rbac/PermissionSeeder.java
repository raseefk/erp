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
        upsertFeature("ADVANCE_PAYMENTS", "Advance Payments", "cash-coin", 8);
        upsertFeature("SYSTEM",     "System",              "cog",         9);
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
        upsertMenu("MENU_ADVANCE_PAYMENTS", "ADVANCE_PAYMENTS", "Advance Payments", "cash-stack", 4);

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
        upsertMenu("MENU_DASHBOARD", "SYSTEM", "Dashboard", "speedometer2", 0);
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
                    String menuId = resolveMenu(id);
                    Menu menu = menuRepo.findById(menuId).orElse(null);
                    permRepo.save(Permission.builder()
                        .id(id)
                        .feature(feature)
                        .menu(menu)
                        .displayName(display)
                        .action(action)
                        .build());
                    created++;
                }
            } else {
                // Force update existing permissions to ensure they are grouped correctly
                permRepo.findById(id).ifPresent(p -> {
                    String fId = resolveFeature(id);
                    String mId = resolveMenu(id);
                    Feature f = featureRepo.findById(fId).orElse(null);
                    Menu m = menuRepo.findById(mId).orElse(null);

                    boolean updated = false;
                    if (f != null && (p.getFeature() == null || !f.getId().equals(p.getFeature().getId()))) {
                        p.setFeature(f);
                        updated = true;
                    }
                    if (m != null && (p.getMenu() == null || !m.getId().equals(p.getMenu().getId()))) {
                        p.setMenu(m);
                        updated = true;
                    }
                    if (updated) permRepo.save(p);
                });
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

    private String resolveMenu(String id) {
        if (id.equals("DASHBOARD_VIEW")) return "MENU_DASHBOARD";
        if (id.startsWith("BILLING_")) return "MENU_BILLING";
        if (id.startsWith("ADVANCE_PAYMENTS_")) return "MENU_ADVANCE_PAYMENTS";
        if (id.startsWith("CRM_CUSTOMERS_")) return "MENU_CUSTOMERS";
        if (id.startsWith("CRM_ENQUIRIES_")) return "MENU_ENQUIRIES";
        if (id.startsWith("INV_")) return "MENU_INVENTORY";
        if (id.startsWith("SCM_PO_")) return "MENU_PURCHASE_ORDERS";
        if (id.startsWith("SCM_VENDORS_")) return "MENU_VENDORS";
        if (id.startsWith("PROJ_LIST_")) return "MENU_PROJECTS";
        if (id.startsWith("PROJ_DAILYLOG_")) return "MENU_SITELOGS";
        if (id.startsWith("PROJ_EXPENSES_") || id.startsWith("PROJ_JOBCARD_")) return "MENU_APPROVALS";
        if (id.startsWith("HR_EMPLOYEES_")) return "MENU_EMPLOYEES";
        if (id.startsWith("HR_ATTENDANCE_")) return "MENU_ATTENDANCE";
        if (id.startsWith("HR_LEAVES_")) return "MENU_LEAVES";
        if (id.startsWith("HR_SALARY_")) return "MENU_SALARIES";
        if (id.startsWith("HR_ATTENDANCE_REPORT_")) return "MENU_ATTENDANCE_REPORT";
        if (id.startsWith("HR_HOLIDAYS_")) return "MENU_HOLIDAYS";
        if (id.startsWith("FIN_EXPENSES_")) return "MENU_EXPENSES";
        if (id.startsWith("FIN_PAYMENTS_")) return "MENU_PAYMENTS";
        if (id.startsWith("FIN_LEDGER_")) return "MENU_PL_REPORT";
        if (id.startsWith("FIN_TRANSACTIONS_") || id.startsWith("FIN_REPORTS_")) return "MENU_PL_REPORT";
        if (id.startsWith("SETTINGS_USERS_")) return "MENU_USERS";
        if (id.startsWith("SETTINGS_ROLES_")) return "MENU_ROLES";
        if (id.startsWith("SETTINGS_COMPANY_") || id.startsWith("SETTINGS_FEATURES_")) return "MENU_SETTINGS";
        return "MENU_SETTINGS";
    }

    private String resolveFeature(String permId) {
        if (permId.equals("DASHBOARD_VIEW")) return "SYSTEM";
        if (permId.startsWith("CRM_")) return "SALES";
        if (permId.startsWith("BILLING_")) return "SALES";
        if (permId.startsWith("ADVANCE_PAYMENTS_")) return "ADVANCE_PAYMENTS";
        if (permId.startsWith("INV_")) return "OPERATIONS";
        if (permId.startsWith("PROJ_")) return "PROJECTS";
        if (permId.startsWith("HR_")) return "HR";
        if (permId.startsWith("FIN_")) return "FINANCE";
        if (permId.startsWith("SETTINGS_COMPANY_") || permId.startsWith("SETTINGS_FEATURES_")) return "SYSTEM";
        if (permId.startsWith("SETTINGS_")) return "ADMIN";
        if (permId.startsWith("SCM_")) return "SCM";
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
