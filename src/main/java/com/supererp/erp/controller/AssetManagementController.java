package com.supererp.erp.controller;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.repository.*;
import com.supererp.erp.service.AssetManagementService;
import com.supererp.erp.service.ProjectService;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/assets")
@RequiredArgsConstructor
@RequiresFeature("ASSETS")
public class AssetManagementController {

    private final AssetManagementService assetService;
    private final VendorRepository vendorRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectService projectService;
    private final AppUserRepository appUserRepo;

    @GetMapping
    @RequiresPermission(Permissions.ASSETS_VIEW)
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String search,
                       Model model) {
        model.addAttribute("assetsPage", assetService.searchAssets(search, page, size));
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        return "assets/list";
    }

    @GetMapping("/new")
    @RequiresPermission(Permissions.ASSETS_MANAGE)
    public String newForm(Model model) {
        model.addAttribute("asset", new Asset());
        populateAssetLookups(model);
        return "assets/form";
    }

    @GetMapping("/{id}/edit")
    @RequiresPermission(Permissions.ASSETS_MANAGE)
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("asset", assetService.getAsset(id));
        populateAssetLookups(model);
        return "assets/form";
    }

    @PostMapping("/save")
    @RequiresPermission(Permissions.ASSETS_MANAGE)
    public String save(@ModelAttribute Asset asset,
                       @RequestParam(required = false) Long vendorId,
                       RedirectAttributes ra) {
        try {
            Asset saved = assetService.saveAsset(asset, vendorId);
            ra.addFlashAttribute("success", "Asset saved.");
            return "redirect:/admin/assets/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/assets";
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission(Permissions.ASSETS_VIEW)
    public String detail(@PathVariable Long id, Model model) {
        Asset asset = assetService.getAsset(id);
        model.addAttribute("asset", asset);
        model.addAttribute("depreciationRows", assetService.depreciationSchedule(id));
        model.addAttribute("currentAssignment", assetService.currentAssignment(id).orElse(null));
        model.addAttribute("assignmentHistory", assetService.assignmentHistory(id));
        model.addAttribute("maintenancePlans", assetService.maintenancePlans(id));
        model.addAttribute("maintenanceJobs", assetService.jobsForAsset(id));
        model.addAttribute("breakdowns", assetService.breakdownsForAsset(id));
        model.addAttribute("mtbfHours", assetService.mtbfHours(id));
        populateAssetLookups(model);
        return "assets/detail";
    }

    @PostMapping("/{id}/depreciation/regenerate")
    @RequiresPermission(Permissions.ASSETS_DEPRECIATION)
    public String regenerateDepreciation(@PathVariable Long id, RedirectAttributes ra) {
        try {
            assetService.regenerateDepreciationSchedule(id);
            ra.addFlashAttribute("success", "Depreciation schedule regenerated.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + id;
    }

    @PostMapping("/{id}/assign")
    @RequiresPermission(Permissions.ASSETS_ASSIGN)
    public String assign(@PathVariable Long id,
                         @RequestParam AssetAssignmentType assignmentType,
                         @RequestParam(required = false) Long employeeId,
                         @RequestParam(required = false) Long projectId,
                         @RequestParam(required = false) String location,
                         @RequestParam(required = false) LocalDate assignedFrom,
                         @RequestParam(required = false) String notes,
                         RedirectAttributes ra) {
        try {
            assetService.assignAsset(id, assignmentType, employeeId, projectId, location, assignedFrom, notes);
            ra.addFlashAttribute("success", "Asset assigned.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + id;
    }

    @PostMapping("/assignments/{assignmentId}/return")
    @RequiresPermission(Permissions.ASSETS_ASSIGN)
    public String returnAssignment(@PathVariable Long assignmentId,
                                   @RequestParam Long assetId,
                                   @RequestParam(required = false) LocalDate returnedAt,
                                   RedirectAttributes ra) {
        try {
            assetService.returnAsset(assignmentId, returnedAt);
            ra.addFlashAttribute("success", "Asset returned.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + assetId;
    }

    @PostMapping("/{id}/maintenance/plans")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String createMaintenancePlan(@PathVariable Long id,
                                        @RequestParam MaintenanceFrequency frequency,
                                        @RequestParam(required = false) Integer customIntervalDays,
                                        @RequestParam LocalDate nextDueDate,
                                        @RequestParam(required = false) Long employeeId,
                                        @RequestParam(required = false) String instructions,
                                        RedirectAttributes ra) {
        try {
            assetService.saveMaintenancePlan(id, frequency, customIntervalDays, nextDueDate, employeeId, instructions);
            ra.addFlashAttribute("success", "Maintenance plan created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + id;
    }

    @GetMapping("/maintenance")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String maintenance(@RequestParam(required = false) LocalDate from,
                              @RequestParam(required = false) LocalDate to,
                              Model model) {
        model.addAttribute("jobs", assetService.maintenanceCalendar(from, to));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "assets/maintenance";
    }

    @PostMapping("/maintenance/generate")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String generateMaintenance(@RequestParam(required = false) LocalDate dueDate,
                                      @RequestParam(defaultValue = "false") boolean createJobCards,
                                      RedirectAttributes ra) {
        int created = assetService.generateDueMaintenanceJobs(dueDate, createJobCards);
        ra.addFlashAttribute("success", "Generated " + created + " maintenance job(s).");
        return "redirect:/admin/assets/maintenance";
    }

    @PostMapping("/maintenance/{jobId}/complete")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String completeMaintenance(@PathVariable Long jobId,
                                      @RequestParam(required = false) LocalDate completedDate,
                                      @RequestParam(required = false) BigDecimal cost,
                                      @RequestParam(required = false) String notes,
                                      RedirectAttributes ra) {
        try {
            assetService.completeMaintenanceJob(jobId, completedDate, cost, notes);
            ra.addFlashAttribute("success", "Maintenance job completed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/maintenance";
    }

    @PostMapping("/{id}/breakdowns")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String reportBreakdown(@PathVariable Long id,
                                  @RequestParam String symptom,
                                  @RequestParam(required = false) Long employeeId,
                                  Authentication authentication,
                                  RedirectAttributes ra) {
        try {
            assetService.reportBreakdown(id, symptom, employeeId, currentUser(authentication));
            ra.addFlashAttribute("success", "Breakdown reported.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + id;
    }

    @PostMapping("/breakdowns/{breakdownId}/close")
    @RequiresPermission(Permissions.ASSETS_MAINTENANCE)
    public String closeBreakdown(@PathVariable Long breakdownId,
                                 @RequestParam Long assetId,
                                 @RequestParam(required = false) String rootCause,
                                 @RequestParam(required = false) String repairAction,
                                 @RequestParam(required = false) BigDecimal repairCost,
                                 RedirectAttributes ra) {
        try {
            assetService.closeBreakdown(breakdownId, rootCause, repairAction, repairCost);
            ra.addFlashAttribute("success", "Breakdown closed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/assets/" + assetId;
    }

    @GetMapping("/analytics")
    @RequiresPermission(Permissions.ASSETS_ANALYTICS)
    public String analytics(Model model) {
        model.addAttribute("summary", assetService.analyticsSummary());
        return "assets/analytics";
    }

    private void populateAssetLookups(Model model) {
        model.addAttribute("AssetStatus", AssetStatus.values());
        model.addAttribute("DepreciationMethod", DepreciationMethod.values());
        model.addAttribute("AssetAssignmentType", AssetAssignmentType.values());
        model.addAttribute("MaintenanceFrequency", MaintenanceFrequency.values());
        model.addAttribute("vendors", vendorRepo.findAllByActiveTrueOrderByNameAsc());
        model.addAttribute("employees", employeeRepo.findByActiveTrueOrderByNameAsc());
        model.addAttribute("projects", projectService.getActive());
    }

    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || TenantContext.getTenantId() == null) return null;
        return appUserRepo.findByUsernameAndTenantId(authentication.getName(), TenantContext.getTenantId()).orElse(null);
    }
}
