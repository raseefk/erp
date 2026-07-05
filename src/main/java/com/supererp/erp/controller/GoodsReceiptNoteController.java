package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.enums.GrnStatus;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.repository.InventoryItemRepository;
import com.supererp.erp.service.ProcurementService;
import com.supererp.erp.service.ScmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Goods Receipt Note controller — handles GRN against POs, 3-way matching.
 */
@Controller
@RequestMapping("/admin/scm/grn")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class GoodsReceiptNoteController {

    private final ProcurementService procurementService;
    private final ScmService scmService;
    private final InventoryItemRepository inventoryRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("grnPage", procurementService.getAllGrns(page, size, q));
        model.addAttribute("q", q);
        model.addAttribute("currentPage", page);
        return "scm/grn-list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long poId, Model model) {
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setReceivedDate(LocalDate.now());
        if (poId != null) {
            PurchaseOrder po = scmService.getPOById(poId);
            grn.setPurchaseOrder(po);
            // Pre-fill items from PO
            po.getItems().forEach(poItem -> {
                GrnItem grnItem = new GrnItem();
                grnItem.setPoItem(poItem);
                grnItem.setInventoryItem(poItem.getInventoryItem());
                grnItem.setDescription(poItem.getDescription());
                grnItem.setUnit(poItem.getUnit());
                grnItem.setOrderedQuantity(poItem.getQuantity());
                grnItem.setReceivedQuantity(poItem.getQuantity());
                grnItem.setAcceptedQuantity(poItem.getQuantity());
                grnItem.setUnitPrice(poItem.getUnitPrice());
                grn.getItems().add(grnItem);
            });
        }
        model.addAttribute("grn", grn);
        model.addAttribute("purchaseOrders", scmService.getAllOrderedPos());
        model.addAttribute("inventoryItems", inventoryRepo.findByActiveTrueOrderByNameAsc());
        model.addAttribute("selectedPoId", poId);
        return "scm/grn-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        GoodsReceiptNote grn = procurementService.getGrnById(id);
        model.addAttribute("grn", grn);
        model.addAttribute("vendorInvoices", procurementService.getInvoicesForPo(grn.getPurchaseOrder().getId()));
        return "scm/grn-detail";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute GoodsReceiptNote grn,
                       RedirectAttributes ra) {
        try {
            procurementService.submitGrn(grn);
            ra.addFlashAttribute("success", "GRN submitted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/grn";
    }

    @PostMapping("/{id}/accept")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> accept(@PathVariable Long id,
                                                  @RequestParam(required = false) String remarks) {
        try {
            procurementService.acceptGrn(id, remarks);
            return ResponseEntity.ok(ApiResponse.ok("GRN accepted and inventory updated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> reject(@PathVariable Long id,
                                                  @RequestParam(required = false) String remarks) {
        try {
            procurementService.rejectGrn(id, remarks);
            return ResponseEntity.ok(ApiResponse.ok("GRN rejected."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
