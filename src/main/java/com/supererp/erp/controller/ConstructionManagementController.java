package com.supererp.erp.controller;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.repository.AppUserRepository;
import com.supererp.erp.service.ConstructionManagementService;
import com.supererp.erp.service.InventoryService;
import com.supererp.erp.service.ProjectService;
import com.supererp.erp.service.VendorService;
import com.supererp.erp.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/construction")
@RequiredArgsConstructor
@RequiresFeature("CONSTRUCTION")
public class ConstructionManagementController {

    private final ConstructionManagementService constructionService;
    private final ProjectService projectService;
    private final InventoryService inventoryService;
    private final VendorService vendorService;
    private final AppUserRepository appUserRepo;

    // ── BOQ Management ──────────────────────────────────────────────────────

    @GetMapping("/boq")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_VIEW)
    public String listBoqs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false) String search,
                           Model model) {
        model.addAttribute("boqsPage", constructionService.searchBoqs(search, page, size));
        model.addAttribute("search", search);
        model.addAttribute("projects", projectService.getActive());
        model.addAttribute("newBoq", new BillOfQuantity());
        return "construction/boq-list";
    }

    @PostMapping("/boq")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_MANAGE)
    public String saveBoq(@ModelAttribute BillOfQuantity boq,
                          @RequestParam Long projectId,
                          RedirectAttributes ra) {
        try {
            boq.setProject(projectService.getById(projectId));
            boq.setStatus(BoqStatus.DRAFT);
            BillOfQuantity saved = constructionService.saveBoq(boq);
            ra.addFlashAttribute("success", "BOQ created.");
            return "redirect:/admin/construction/boq/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/construction/boq";
        }
    }

    @GetMapping("/boq/{boqId}")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_VIEW)
    public String boqDetail(@PathVariable Long boqId, Model model) {
        BillOfQuantity boq = constructionService.getBoq(boqId);
        model.addAttribute("boq", boq);
        model.addAttribute("items", constructionService.itemsForBoq(boqId));
        model.addAttribute("newItem", new BoqItem());
        model.addAttribute("inventoryItems", inventoryService.getAll());
        return "construction/boq-detail";
    }

    @PostMapping("/boq/{boqId}/items")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_MANAGE)
    public String saveBoqItem(@PathVariable Long boqId,
                              @ModelAttribute BoqItem item,
                              @RequestParam(required = false) Long inventoryItemId,
                              RedirectAttributes ra) {
        try {
            item.setId(null); // Explicitly ensure we are creating a new item
            item.setBoq(constructionService.getBoq(boqId));
            if (inventoryItemId != null) {
                item.setInventoryItem(inventoryService.getById(inventoryItemId));
            }
            item.setAmount(item.getQuantity().multiply(item.getRate()));
            item.setStatus(BoqItemStatus.NOT_STARTED);
            item.setCompletedQuantity(BigDecimal.ZERO);
            constructionService.saveBoqItem(item);
            ra.addFlashAttribute("success", "BOQ Item added.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/boq/" + boqId;
    }

    @PostMapping("/boq/{boqId}/approve")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_MANAGE)
    public String approveBoq(@PathVariable Long boqId, RedirectAttributes ra) {
        try {
            constructionService.approveBoq(boqId);
            ra.addFlashAttribute("success", "BOQ approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/boq/" + boqId;
    }

    @PostMapping("/boq/items/{itemId}/progress")
    @RequiresPermission(Permissions.CONSTRUCTION_BOQ_PROGRESS)
    public String recordBoqProgress(@PathVariable Long itemId,
                                    @RequestParam Long boqId,
                                    @RequestParam BigDecimal completedQuantity,
                                    @RequestParam(required = false) LocalDate progressDate,
                                    @RequestParam(required = false) String remarks,
                                    Authentication auth,
                                    RedirectAttributes ra) {
        try {
            constructionService.recordProgress(itemId, completedQuantity, progressDate, null, remarks, currentUser(auth));
            ra.addFlashAttribute("success", "Progress recorded.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/boq/" + boqId;
    }

    // ── Material at Site ──────────────────────────────────────────────────────

    @GetMapping("/materials")
    @RequiresPermission(Permissions.CONSTRUCTION_MATERIAL_SITE_VIEW)
    public String materialRegister(@RequestParam(required = false) Long projectId, Model model) {
        model.addAttribute("projects", projectService.getActive());
        if (projectId != null) {
            Project project = projectService.getById(projectId);
            model.addAttribute("selectedProject", project);
            List<MaterialSiteTransaction> transactions = constructionService.materialsForProject(projectId);
            model.addAttribute("transactions", transactions);

            // Group by material and calculate balance
            Map<Long, Map<String, Object>> summary = new HashMap<>();
            for (MaterialSiteTransaction tx : transactions) {
                Long itemId = tx.getInventoryItem().getId();
                summary.computeIfAbsent(itemId, k -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", tx.getInventoryItem().getName());
                    map.put("unit", tx.getUnit());
                    map.put("received", BigDecimal.ZERO);
                    map.put("consumed", BigDecimal.ZERO);
                    map.put("returned", BigDecimal.ZERO);
                    map.put("adjustments", BigDecimal.ZERO);
                    map.put("balance", BigDecimal.ZERO);
                    return map;
                });

                Map<String, Object> itemData = summary.get(itemId);
                BigDecimal qty = tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO;
                
                switch (tx.getTransactionType()) {
                    case RECEIPT -> {
                        itemData.put("received", ((BigDecimal) itemData.get("received")).add(qty));
                        itemData.put("balance", ((BigDecimal) itemData.get("balance")).add(qty));
                    }
                    case CONSUMPTION -> {
                        itemData.put("consumed", ((BigDecimal) itemData.get("consumed")).add(qty));
                        itemData.put("balance", ((BigDecimal) itemData.get("balance")).subtract(qty));
                    }
                    case RETURN -> {
                        itemData.put("returned", ((BigDecimal) itemData.get("returned")).add(qty));
                        itemData.put("balance", ((BigDecimal) itemData.get("balance")).subtract(qty));
                    }
                    case ADJUSTMENT -> {
                        itemData.put("adjustments", ((BigDecimal) itemData.get("adjustments")).add(qty));
                        itemData.put("balance", ((BigDecimal) itemData.get("balance")).add(qty));
                    }
                }
            }
            model.addAttribute("stockSummary", summary.values());
        }
        model.addAttribute("inventoryItems", inventoryService.getAll());
        if (projectId != null) {
            model.addAttribute("boqItems", constructionService.getBoqItemsByProject(projectId));
            model.addAttribute("boqs", constructionService.getBoqsByProject(projectId));
        }
        model.addAttribute("MaterialSiteTransactionType", MaterialSiteTransactionType.values());
        return "construction/material-register";
    }

    @PostMapping("/materials")
    @RequiresPermission(Permissions.CONSTRUCTION_MATERIAL_SITE_MANAGE)
    public String recordMaterial(@RequestParam(required = false) Long projectId,
                                 @RequestParam(required = false) Long inventoryItemId,
                                 @RequestParam(required = false) Long boqItemId,
                                 @RequestParam(required = false) MaterialSiteTransactionType transactionType,
                                 @RequestParam(required = false) BigDecimal quantity,
                                 @RequestParam(required = false) String unit,
                                 @RequestParam(required = false) LocalDate transactionDate,
                                 @RequestParam(required = false) String remarks,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        Long finalInventoryItemId = inventoryItemId;
        if (finalInventoryItemId == null && boqItemId != null) {
            BoqItem bItem = constructionService.getBoqItem(boqItemId);
            if (bItem.getInventoryItem() != null) {
                finalInventoryItemId = bItem.getInventoryItem().getId();
            }
        }

        if (projectId == null || finalInventoryItemId == null || transactionType == null || quantity == null || unit == null) {
            ra.addFlashAttribute("error", "Missing required fields. Please select a BOQ item and enter quantity.");
            return "redirect:/admin/construction/materials" + (projectId != null ? "?projectId=" + projectId : "");
        }
        try {
            MaterialSiteTransaction tx = MaterialSiteTransaction.builder()
                .project(projectService.getById(projectId))
                .inventoryItem(inventoryService.getById(finalInventoryItemId))
                .boqItem(boqItemId != null ? constructionService.getBoqItem(boqItemId) : null)
                .transactionType(transactionType)
                .quantity(quantity)
                .unit(unit)
                .transactionDate(transactionDate != null ? transactionDate : LocalDate.now())
                .remarks(remarks)
                .recordedBy(currentUser(auth))
                .build();
            constructionService.recordMaterialTransaction(tx);
            ra.addFlashAttribute("success", "Material transaction recorded.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/materials?projectId=" + projectId;
    }

    // ── Subcontractor Running Bills ───────────────────────────────────────────

    @GetMapping("/running-bills")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    public String listRunningBills(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String search,
                                   Model model) {
        model.addAttribute("billsPage", constructionService.searchRunningBills(search, page, size));
        model.addAttribute("search", search);
        model.addAttribute("projects", projectService.getActive());
        model.addAttribute("vendors", vendorService.getActive());
        model.addAttribute("jobCards", constructionService.getActiveJobCards());
        return "construction/bill-list";
    }

    @PostMapping("/running-bills")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    public String createRunningBill(@RequestParam Long projectId,
                                    @RequestParam Long vendorId,
                                    @RequestParam String billNumber,
                                    @RequestParam LocalDate billDate,
                                    @RequestParam LocalDate periodFrom,
                                    @RequestParam LocalDate periodTo,
                                    @RequestParam(required = false) Long jobCardId,
                                    RedirectAttributes ra) {
        try {
            SubcontractorRunningBill saved = constructionService.createRunningBill(
                projectId, vendorId, billNumber, billDate, periodFrom, periodTo, jobCardId);
            ra.addFlashAttribute("success", "Running Bill created.");
            return "redirect:/admin/construction/running-bills/" + saved.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/construction/running-bills";
        }
    }

    @GetMapping("/running-bills/{id}")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    public String runningBillDetail(@PathVariable Long id, Model model) {
        SubcontractorRunningBill bill = constructionService.getRunningBill(id);
        model.addAttribute("bill", bill);
        model.addAttribute("items", bill.getItems());
        return "construction/bill-detail";
    }

    @GetMapping("/running-bills/{id}/pdf")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> downloadSubcontractorBillPdf(@PathVariable Long id) {
        SubcontractorRunningBill bill = constructionService.getRunningBill(id);
        byte[] pdf = constructionService.generateRunningBillPdf(id);
        String filename = "Bill_" + bill.getBillNumber().replace(" ", "_") + ".pdf";
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @PostMapping("/running-bills/{id}/items")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    public String addRunningBillItem(@PathVariable Long id,
                                     @RequestParam String description,
                                     @RequestParam BigDecimal claimedQuantity,
                                     @RequestParam BigDecimal rate,
                                     RedirectAttributes ra) {
        try {
            SubcontractorRunningBill bill = constructionService.getRunningBill(id);
            SubcontractorRunningBillItem item = SubcontractorRunningBillItem.builder()
                .runningBill(bill)
                .description(description)
                .claimedQuantity(claimedQuantity)
                .certifiedQuantity(BigDecimal.ZERO)
                .rate(rate)
                .claimedAmount(claimedQuantity.multiply(rate))
                .certifiedAmount(BigDecimal.ZERO)
                .build();
            constructionService.saveRunningBillItem(item);
            ra.addFlashAttribute("success", "Item added to bill.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/running-bills/" + id;
    }

    @PostMapping("/running-bills/{id}/submit")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_VIEW)
    public String submitRunningBill(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            constructionService.submitRunningBill(id, currentUser(auth));
            ra.addFlashAttribute("success", "Bill submitted for certification.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/running-bills/" + id;
    }

    @PostMapping("/running-bills/{id}/certify")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_CERTIFY)
    public String certifyRunningBill(@PathVariable Long id,
                                     @RequestParam Map<String, String> params,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        try {
            Map<Long, BigDecimal> certified = new HashMap<>();
            params.forEach((k, v) -> {
                if (k.startsWith("certQty_")) {
                    Long itemId = Long.parseLong(k.replace("certQty_", ""));
                    certified.put(itemId, new BigDecimal(v));
                }
            });
            constructionService.certifyRunningBill(id, certified, currentUser(auth));
            ra.addFlashAttribute("success", "Bill successfully certified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/running-bills/" + id;
    }

    @PostMapping("/running-bills/{id}/reject")
    @RequiresPermission(Permissions.CONSTRUCTION_SUBCONTRACTOR_BILL_CERTIFY)
    public String rejectRunningBill(@PathVariable Long id, @RequestParam String reason, RedirectAttributes ra) {
        try {
            constructionService.rejectRunningBill(id, reason);
            ra.addFlashAttribute("success", "Bill rejected.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/running-bills/" + id;
    }

    @PostMapping("/running-bills/{id}/pay")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_PAYMENT_RELEASE)
    public String payRunningBill(@PathVariable Long id, RedirectAttributes ra) {
        try {
            constructionService.markRunningBillPaid(id);
            ra.addFlashAttribute("success", "Bill marked as paid.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/running-bills/" + id;
    }

    // ── Milestones ────────────────────────────────────────────────────────────

    @GetMapping("/milestones")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_VIEW)
    public String listMilestones(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String search,
                                 Model model) {
        model.addAttribute("milestonesPage", constructionService.searchMilestones(search, page, size));
        model.addAttribute("search", search);
        model.addAttribute("projects", projectService.getActive());
        model.addAttribute("newMilestone", new ProjectMilestone());
        return "construction/milestone-list";
    }

    @PostMapping("/milestones")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_APPROVE)
    public String saveMilestone(@ModelAttribute ProjectMilestone milestone,
                                @RequestParam Long projectId,
                                RedirectAttributes ra) {
        try {
            milestone.setProject(projectService.getById(projectId));
            milestone.setStatus(ProjectMilestoneStatus.PLANNED);
            constructionService.saveMilestone(milestone);
            ra.addFlashAttribute("success", "Milestone created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/milestones";
    }

    @PostMapping("/milestones/{id}/submit")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_APPROVE)
    public String submitMilestone(@PathVariable Long id, RedirectAttributes ra) {
        try {
            constructionService.submitMilestone(id);
            ra.addFlashAttribute("success", "Milestone submitted for client approval.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/milestones";
    }

    @PostMapping("/milestones/{id}/approve")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_APPROVE)
    public String approveMilestone(@PathVariable Long id, @RequestParam String reference, RedirectAttributes ra) {
        try {
            constructionService.approveMilestone(id, reference);
            ra.addFlashAttribute("success", "Milestone client-approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/milestones";
    }

    @PostMapping("/milestones/{id}/release")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_PAYMENT_RELEASE)
    public String releaseMilestonePayment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            constructionService.releaseMilestonePayment(id);
            ra.addFlashAttribute("success", "Payment release approved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/construction/milestones";
    }

    @GetMapping("/milestones/{id}/pdf")
    @RequiresPermission(Permissions.CONSTRUCTION_MILESTONE_VIEW)
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> downloadMilestonePdf(@PathVariable Long id) {
        ProjectMilestone milestone = constructionService.getMilestone(id);
        byte[] pdf = constructionService.generateMilestonePdf(id);
        String filename = "Milestone_" + milestone.getId() + ".pdf";
        return org.springframework.http.ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .body(pdf);
    }


    private AppUser currentUser(Authentication authentication) {
        if (authentication == null || TenantContext.getTenantId() == null) return null;
        return appUserRepo.findByUsernameAndTenantId(authentication.getName(), TenantContext.getTenantId()).orElse(null);
    }
}
