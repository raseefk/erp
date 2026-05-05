package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.InventoryItem;
import com.levanto.flooring.entity.PurchaseOrder;
import com.levanto.flooring.entity.PurchaseOrderItem;
import com.levanto.flooring.enums.PurchaseOrderStatus;
import com.levanto.flooring.repository.InventoryItemRepository;
import com.levanto.flooring.service.ScmService;
import com.levanto.flooring.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;

@Controller @RequestMapping("/admin/scm/po") @RequiredArgsConstructor
public class PurchaseOrderController {

    private final ScmService scmService;
    private final VendorService vendorService;
    private final InventoryItemRepository itemRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="20") int size,
                       @RequestParam(required=false) String q,
                       Model m) {
        m.addAttribute("poPage",      scmService.getAllPOs(page, size, q));
        m.addAttribute("q",           q);
        m.addAttribute("currentPage", page);
        return "scm/po-list";
    }

    @GetMapping("/new")
    public String newForm(Model m) {
        PurchaseOrder po = new PurchaseOrder();
        po.setOrderDate(LocalDate.now());
        po.setPoNumber("PO-" + System.currentTimeMillis() / 1000); // Simple generator
        
        m.addAttribute("po", po);
        m.addAttribute("vendors", vendorService.getActive());
        m.addAttribute("inventoryItems", itemRepo.findByActiveTrueOrderByNameAsc());
        return "scm/po-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        m.addAttribute("po", scmService.getPOById(id));
        m.addAttribute("vendors", vendorService.getActive());
        m.addAttribute("inventoryItems", itemRepo.findByActiveTrueOrderByNameAsc());
        return "scm/po-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model m) {
        m.addAttribute("po", scmService.getPOById(id));
        return "scm/po-detail";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PurchaseOrder po, RedirectAttributes ra) {
        try {
            scmService.savePO(po);
            ra.addFlashAttribute("success", "Purchase Order saved.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/po";
    }

    @PostMapping("/{id}/status") @ResponseBody
    public ResponseEntity<ApiResponse<?>> updateStatus(@PathVariable Long id, @RequestParam PurchaseOrderStatus status) {
        try {
            scmService.updatePOStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("Status updated to " + status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/mark-paid") @ResponseBody
    public ResponseEntity<ApiResponse<?>> markPaid(@PathVariable Long id) {
        try {
            scmService.markAsPaid(id);
            return ResponseEntity.ok(ApiResponse.ok("Purchase Order marked as paid and expense entry created."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
