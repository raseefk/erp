package com.supererp.erp.controller.system;

import com.supererp.erp.rbac.annotation.AuditAction;
import com.supererp.erp.rbac.entity.Feature;
import com.supererp.erp.rbac.repository.FeatureRepository;
import com.supererp.erp.rbac.repository.MenuRepository;
import com.supererp.erp.rbac.service.RbacService;
import com.supererp.erp.repository.AppUserRepository;
import com.supererp.erp.service.CompanySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Application administration controller — single-tenant mode.
 *
 * Previously the "system admin" portal managed multiple tenants.
 * Now this controller manages the single application: features, menus,
 * users, roles, and system health — all accessible to ADMIN role users.
 *
 * URL prefix changed from /system to /app-admin to disambiguate from
 * the tenant dashboard.
 */
@Controller
@RequestMapping("/app-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class SystemAdminController {

    private final RbacService           rbacService;
    private final FeatureRepository     featureRepo;
    private final MenuRepository        menuRepo;
    private final AppUserRepository     appUserRepository;
    private final CompanySettingsService companySettingsService;

    // ── Application Health Dashboard ─────────────────────────────────────────
    @GetMapping({"", "/", "/dashboard"})
    public String appDashboard(Model model) {
        // OS Metrics
        com.sun.management.OperatingSystemMXBean osBean =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = Math.max(0.0, osBean.getSystemCpuLoad() * 100.0);
        double totalRamGb = osBean.getTotalMemorySize() / (1024.0 * 1024.0 * 1024.0);
        double freeRamGb  = osBean.getFreeMemorySize()  / (1024.0 * 1024.0 * 1024.0);
        double usedRamGb  = totalRamGb - freeRamGb;
        double ramPercent = totalRamGb > 0 ? (usedRamGb / totalRamGb) * 100.0 : 0.0;

        // JVM Heap
        var memBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed();
        long heapMax  = memBean.getHeapMemoryUsage().getMax();
        double jvmHeapPercent = heapMax > 0 ? (heapUsed / (double) heapMax) * 100.0 : 0.0;

        // User count
        long totalUsers   = appUserRepository.count();
        long enabledUsers = appUserRepository.findAllByEnabledTrueOrderByFullNameAsc().size();

        // Feature stats
        Set<String> enabledFeatures = rbacService.getEnabledFeatures();
        long totalFeatures   = featureRepo.count();
        long enabledFeatCount = enabledFeatures.size();
        List<com.supererp.erp.rbac.entity.Feature> allFeatures = featureRepo.findAllWithMenus();

        model.addAttribute("cpuLoad",        String.format("%.1f", cpuLoad));
        model.addAttribute("usedRamGb",      String.format("%.1f", usedRamGb));
        model.addAttribute("totalRamGb",     String.format("%.1f", totalRamGb));
        model.addAttribute("ramPercent",     String.format("%.1f", ramPercent));
        model.addAttribute("jvmHeapPercent", String.format("%.1f", jvmHeapPercent));
        model.addAttribute("heapUsedMb",     String.format("%.0f", heapUsed / (1024.0 * 1024.0)));
        model.addAttribute("heapMaxMb",      String.format("%.0f", heapMax  / (1024.0 * 1024.0)));
        model.addAttribute("totalUsers",     totalUsers);
        model.addAttribute("enabledUsers",   enabledUsers);
        model.addAttribute("totalFeatures",  totalFeatures);
        model.addAttribute("enabledFeatureCount", enabledFeatCount);
        model.addAttribute("allFeatures",    allFeatures);
        model.addAttribute("enabledFeatures", enabledFeatures);
        model.addAttribute("companySettings", companySettingsService.getSettings());
        model.addAttribute("pageTitle", "Application Dashboard");
        model.addAttribute("activePage", "system-dashboard");
        return "system/dashboard";
    }

    // ── Live Server Stats (SSE) ───────────────────────────────────────────────
    @GetMapping(value = "/api/stats", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter liveStats() {
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(0L);
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.sun.management.OperatingSystemMXBean osBean =
                    (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
                var memBean = ManagementFactory.getMemoryMXBean();
                while (true) {
                    double cpu       = Math.max(0.0, osBean.getSystemCpuLoad() * 100.0);
                    double totalRam  = osBean.getTotalMemorySize() / (1024.0 * 1024.0 * 1024.0);
                    double usedRam   = totalRam - osBean.getFreeMemorySize() / (1024.0 * 1024.0 * 1024.0);
                    long   heapUsed  = memBean.getHeapMemoryUsage().getUsed();
                    long   heapMax   = memBean.getHeapMemoryUsage().getMax();

                    Map<String, Object> stats = Map.of(
                        "cpuLoad",        String.format("%.1f", cpu),
                        "ramPercent",     String.format("%.1f", totalRam > 0 ? (usedRam / totalRam) * 100.0 : 0.0),
                        "usedRamGb",      String.format("%.1f", usedRam),
                        "totalRamGb",     String.format("%.1f", totalRam),
                        "jvmHeapPercent", String.format("%.1f", heapMax > 0 ? (heapUsed / (double) heapMax) * 100.0 : 0.0),
                        "heapUsedMb",     String.format("%.0f", heapUsed / (1024.0 * 1024.0)),
                        "heapMaxMb",      String.format("%.0f", heapMax  / (1024.0 * 1024.0))
                    );
                    emitter.send(stats, org.springframework.http.MediaType.APPLICATION_JSON);
                    Thread.sleep(3000);
                }
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    // ── Feature & Menu Management ─────────────────────────────────────────────
    @GetMapping("/features")
    public String featuresPage(Model model) {
        List<Feature> allFeatures = featureRepo.findAllWithMenus();
        Set<String>   enabled     = rbacService.getEnabledFeatures();
        List<String>  disabledMenuIds = rbacService.getMenuMappingsForApplication().stream()
            .filter(m -> !m.isEnabled()).map(m -> m.getMenuId()).toList();

        model.addAttribute("allFeatures",    allFeatures);
        model.addAttribute("enabledFeatures", enabled);
        model.addAttribute("disabledMenuIds", disabledMenuIds);
        model.addAttribute("pageTitle",       "Feature & Menu Management");
        model.addAttribute("activePage",      "system-features");
        return "system/features";
    }

    @PostMapping("/features")
    @AuditAction(value = "APP_FEATURE_UPDATE", entityType = "ApplicationSettings")
    public String updateFeatures(
            @RequestParam(name = "featureIds", required = false) List<String> featureIds,
            @RequestParam(name = "menuIds",    required = false) List<String> menuIds,
            RedirectAttributes ra) {
        try {
            List<String> enabledFeatIds = featureIds != null ? featureIds : List.of();
            List<String> enabledMenuIds = menuIds    != null ? menuIds    : List.of();

            featureRepo.findAll().forEach(f ->
                rbacService.toggleFeature(f.getId(), enabledFeatIds.contains(f.getId())));

            menuRepo.findAll().forEach(m ->
                rbacService.toggleMenu(m.getId(), enabledMenuIds.contains(m.getId())));

            ra.addFlashAttribute("success", "Feature and menu settings updated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app-admin/features";
    }
}
