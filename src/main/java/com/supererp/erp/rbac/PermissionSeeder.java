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
        upsertFeature("SALES",            "Sales",                    "receipt",           "Enquiries, billing, quotations, customers",     1);
        upsertFeature("OPERATIONS",       "Operations",               "boxes",             "Inventory, stock management",                   2);
        upsertFeature("SCM",              "Supply Chain",             "truck",             "Vendors, POs, GRN, RFQ, rate contracts, approvals", 3);
        upsertFeature("PROJECTS",         "Projects",                 "folder-open",       "Project tracking, site logs, approvals",        4);
        upsertFeature("ASSETS",           "Asset Management",         "tools",             "Asset register, maintenance, depreciation",     5);
        upsertFeature("CONSTRUCTION",     "Construction",             "building",          "BOQ, subcontractor bills, milestones, materials",6);
        upsertFeature("HR",               "Human Resources",          "id-card",           "Employees, attendance, leaves, salaries",       7);
        upsertFeature("FINANCE",          "Finance",                  "coins",             "Expenses, payments, P&L reporting",             8);
        upsertFeature("ADMIN",            "Administration",           "shield-lock",       "Users, roles, permissions management",          9);
        upsertFeature("ADVANCE_PAYMENTS", "Advance Payments",         "cash-coin",         "Customer and vendor advance payment tracking",  10);
        upsertFeature("DMS",              "Document Management",      "cloud-arrow-up",    "Document vault, version control, expiry alerts",11);
        upsertFeature("SYSTEM",           "System",                   "cog",               "Dashboard, company settings, holidays",         12);
    }

    private void upsertFeature(String id, String name, String icon, String description, int order) {
        featureRepo.findById(id).ifPresentOrElse(f -> {
            boolean changed = false;
            if (!name.equals(f.getDisplayName()))        { f.setDisplayName(name); changed = true; }
            if (!icon.equals(f.getIcon()))               { f.setIcon(icon);        changed = true; }
            if (f.getDescription() == null || f.getDescription().isBlank()) { f.setDescription(description); changed = true; }
            if (changed) featureRepo.save(f);
        }, () -> featureRepo.save(Feature.builder()
                .id(id).displayName(name).icon(icon)
                .description(description).sortOrder(order).build()));
    }

    private void seedMenus() {
        // Sales
        upsertMenu("MENU_ENQUIRIES", "SALES", "Enquiries", "chat-left-text", 1);
        upsertMenu("MENU_BILLING",   "SALES", "Billing & Quotations", "receipt-cutoff", 2);
        upsertMenu("MENU_CUSTOMERS", "SALES", "Customers", "people", 3);
        upsertMenu("MENU_ADVANCE_PAYMENTS", "ADVANCE_PAYMENTS", "Advance Payments", "cash-stack", 4);

        // Operations
        upsertMenu("MENU_INVENTORY", "OPERATIONS", "Inventory", "boxes", 1);
        upsertMenu("MENU_ASSETS", "ASSETS", "Asset Register", "tools", 1);
        upsertMenu("MENU_ASSET_MAINTENANCE", "ASSETS", "Maintenance", "calendar-check", 2);
        upsertMenu("MENU_ASSET_ANALYTICS", "ASSETS", "Asset Analytics", "bar-chart-line", 3);

        // Supply Chain — existing
        upsertMenu("MENU_VENDORS",          "SCM", "Vendor Directory",       "truck",                  1);
        upsertMenu("MENU_PURCHASE_ORDERS",  "SCM", "Purchase Orders",        "cart",                   2);
        // Supply Chain — new SCM menus
        upsertMenu("MENU_GRN",              "SCM", "Goods Receipt (GRN)",    "box-seam",               3);
        upsertMenu("MENU_VENDOR_INVOICES",  "SCM", "Vendor Invoices",        "file-earmark-check",     4);
        upsertMenu("MENU_RFQ",              "SCM", "Request for Quotation",  "envelope-open-text",     5);
        upsertMenu("MENU_BLANKET_PO",       "SCM", "Blanket POs",            "file-earmark-ruled",     6);
        upsertMenu("MENU_PROCUREMENT_APPROVALS", "SCM", "PO Approvals",      "check2-circle",          7);
        upsertMenu("MENU_VENDOR_SCORECARD", "SCM", "Vendor Scorecard",       "star-half",              8);

        // Projects
        upsertMenu("MENU_PROJECTS", "PROJECTS", "Projects", "kanban", 1);
        upsertMenu("MENU_SITELOGS", "PROJECTS", "Daily Logs", "journal-text", 2);
        upsertMenu("MENU_APPROVALS", "PROJECTS", "Approval Queue", "check2-square", 3);
        upsertMenu("MENU_BOQ", "CONSTRUCTION", "BOQ", "clipboard-data", 1);
        upsertMenu("MENU_MATERIAL_SITE", "CONSTRUCTION", "Material at Site", "bricks", 2);
        upsertMenu("MENU_SUBCONTRACTOR_BILLS", "CONSTRUCTION", "Subcontractor Bills", "file-earmark-check", 3);
        upsertMenu("MENU_MILESTONES", "CONSTRUCTION", "Milestones", "flag", 4);

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

        // Document Management System
        upsertMenu("MENU_DMS_VAULT",            "DMS", "Document Vault",        "cloud-arrow-up",   1);
        upsertMenu("MENU_DMS_FOLDERS",          "DMS", "Manage Folders",        "folder2-open",     2);
        upsertMenu("MENU_DMS_DIGITAL_SIGNATURE","DMS", "Digital Signatures",    "pen",              3);
        upsertMenu("MENU_DMS_EXPIRY_ALERTS",    "DMS", "Expiry Alerts",         "clock-history",    4);

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
        if (id.equals("ASSETS_VIEW") || id.equals("ASSETS_MANAGE") || id.equals("ASSETS_ASSIGN") || id.equals("ASSETS_DEPRECIATION")) return "MENU_ASSETS";
        if (id.equals("ASSETS_MAINTENANCE")) return "MENU_ASSET_MAINTENANCE";
        if (id.equals("ASSETS_ANALYTICS")) return "MENU_ASSET_ANALYTICS";
        // SCM — existing
        if (id.startsWith("SCM_PO_")) return "MENU_PURCHASE_ORDERS";
        if (id.startsWith("SCM_VENDORS_")) return "MENU_VENDORS";
        // SCM — new
        if (id.startsWith("SCM_GRN_")) return "MENU_GRN";
        if (id.startsWith("SCM_INVOICE_")) return "MENU_VENDOR_INVOICES";
        if (id.startsWith("SCM_RFQ_")) return "MENU_RFQ";
        if (id.startsWith("SCM_BLANKET_PO_")) return "MENU_BLANKET_PO";
        if (id.startsWith("SCM_APPROVAL_")) return "MENU_PROCUREMENT_APPROVALS";
        if (id.startsWith("SCM_VENDOR_RATING_")) return "MENU_VENDOR_SCORECARD";
        if (id.startsWith("SCM_LANDED_COST_")) return "MENU_PURCHASE_ORDERS";
        // DMS
        if (id.startsWith("DMS_DOCUMENTS_") || id.startsWith("DMS_VERSIONS_")) return "MENU_DMS_VAULT";
        if (id.startsWith("DMS_FOLDERS_")) return "MENU_DMS_FOLDERS";
        if (id.startsWith("DMS_DIGITAL_SIGNATURE_")) return "MENU_DMS_DIGITAL_SIGNATURE";
        if (id.startsWith("DMS_EXPIRY_ALERTS_")) return "MENU_DMS_EXPIRY_ALERTS";
        // Projects
        if (id.startsWith("PROJ_LIST_")) return "MENU_PROJECTS";
        if (id.startsWith("PROJ_DAILYLOG_")) return "MENU_SITELOGS";
        if (id.startsWith("PROJ_EXPENSES_") || id.startsWith("PROJ_JOBCARD_")) return "MENU_APPROVALS";
        if (id.startsWith("CONSTRUCTION_BOQ_")) return "MENU_BOQ";
        if (id.startsWith("CONSTRUCTION_MATERIAL_SITE_")) return "MENU_MATERIAL_SITE";
        if (id.startsWith("CONSTRUCTION_SUBCONTRACTOR_BILL_")) return "MENU_SUBCONTRACTOR_BILLS";
        if (id.startsWith("CONSTRUCTION_MILESTONE_")) return "MENU_MILESTONES";
        if (id.startsWith("HR_EMPLOYEES_")) return "MENU_EMPLOYEES";
        if (id.startsWith("HR_ATTENDANCE_REPORT_")) return "MENU_ATTENDANCE_REPORT";
        if (id.startsWith("HR_ATTENDANCE_")) return "MENU_ATTENDANCE";
        if (id.startsWith("HR_LEAVES_")) return "MENU_LEAVES";
        if (id.startsWith("HR_SALARY_")) return "MENU_SALARIES";
        if (id.startsWith("HR_HOLIDAYS_")) return "MENU_HOLIDAYS";
        if (id.startsWith("FIN_EXPENSES_")) return "MENU_EXPENSES";
        if (id.startsWith("FIN_PAYMENTS_")) return "MENU_PAYMENTS";
        if (id.equals("FINANCE_VIEW") || id.startsWith("FIN_LEDGER_")) return "MENU_PL_REPORT";
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
        if (permId.startsWith("ASSETS_")) return "ASSETS";
        if (permId.startsWith("PROJ_")) return "PROJECTS";
        if (permId.startsWith("CONSTRUCTION_")) return "CONSTRUCTION";
        if (permId.startsWith("HR_")) return "HR";
        if (permId.equals("FINANCE_VIEW") || permId.startsWith("FIN_")) return "FINANCE";
        if (permId.startsWith("SETTINGS_COMPANY_") || permId.startsWith("SETTINGS_FEATURES_")) return "SYSTEM";
        if (permId.startsWith("SETTINGS_")) return "ADMIN";
        if (permId.startsWith("SCM_")) return "SCM";
        if (permId.startsWith("DMS_")) return "DMS";
        return permId.split("_")[0];
    }

    private String resolveAction(String permId) {
        // Last segment = action
        String[] parts = permId.split("_");
        return parts[parts.length - 1];
    }

    private String toDisplayName(String permId) {
        // Proper human-readable names
        Map<String, String> names = new LinkedHashMap<>();
        // SCM — new
        names.put("SCM_GRN_VIEW",              "View GRN");
        names.put("SCM_GRN_CREATE",            "Create GRN");
        names.put("SCM_GRN_ACCEPT",            "Accept / Reject GRN");
        names.put("SCM_INVOICE_VIEW",          "View Vendor Invoices");
        names.put("SCM_INVOICE_CREATE",        "Submit Vendor Invoice");
        names.put("SCM_RFQ_VIEW",              "View RFQs");
        names.put("SCM_RFQ_MANAGE",            "Manage RFQs");
        names.put("SCM_BLANKET_PO_VIEW",       "View Blanket POs");
        names.put("SCM_BLANKET_PO_MANAGE",     "Manage Blanket POs");
        names.put("SCM_APPROVAL_VIEW",         "View PO Approvals");
        names.put("SCM_APPROVAL_MANAGE",       "Approve / Reject POs");
        names.put("SCM_VENDOR_RATING_VIEW",    "View Vendor Scorecard");
        names.put("SCM_VENDOR_RATING_MANAGE",  "Rate Vendors");
        names.put("SCM_LANDED_COST_MANAGE",    "Manage Landed Costs");
        // DMS
        names.put("DMS_DOCUMENTS_VIEW",        "View Documents");
        names.put("DMS_DOCUMENTS_UPLOAD",      "Upload Documents");
        names.put("DMS_DOCUMENTS_EDIT",        "Edit Documents");
        names.put("DMS_DOCUMENTS_DELETE",      "Delete Documents");
        names.put("DMS_FOLDERS_MANAGE",        "Manage Folders");
        names.put("DMS_VERSIONS_VIEW",         "View Version History");
        names.put("DMS_DIGITAL_SIGNATURE_VIEW",   "View Digital Signatures");
        names.put("DMS_DIGITAL_SIGNATURE_SIGN",   "Sign Documents");
        names.put("DMS_DIGITAL_SIGNATURE_VERIFY", "Verify Signatures");
        names.put("DMS_EXPIRY_ALERTS_VIEW",       "View Expiry Alerts");
        names.put("DMS_EXPIRY_ALERTS_MANAGE",     "Manage Expiry Alerts");
        if (names.containsKey(permId)) return names.get(permId);
        // Generic fallback
        return permId.replace("_", " ").toLowerCase();
    }
}
