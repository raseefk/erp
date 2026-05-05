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
    private final PermissionRepository  permRepo;

    @Override
    public void run(String... args) {
        seedFeatures();
        seedPermissions();
        log.info("✅ Permission/Feature/Menu schema seeded");
    }

    private void seedFeatures() {
        upsertFeature("BILLING",   "Billing & Invoicing",      "receipt",     1);
        upsertFeature("CRM",       "Customers & Enquiries",    "users",       2);
        upsertFeature("INVENTORY", "Inventory",                "boxes",       3);
        upsertFeature("SCM",       "Supply Chain",             "truck",       4);
        upsertFeature("PROJECTS",  "Project Management",       "folder-open", 5);
        upsertFeature("HR",        "Human Resources",          "id-card",     6);
        upsertFeature("FINANCE",   "Finance",                  "coins",       7);
        upsertFeature("SETTINGS",  "Settings",                 "cog",         8);
    }

    private void upsertFeature(String id, String name, String icon, int order) {
        if (!featureRepo.existsById(id)) {
            featureRepo.save(Feature.builder()
                .id(id).displayName(name).icon(icon).sortOrder(order).build());
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
