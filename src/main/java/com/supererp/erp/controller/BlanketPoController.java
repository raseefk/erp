package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.repository.InventoryItemRepository;
import com.supererp.erp.service.ProcurementService;
import com.supererp.erp.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/scm/blanket-po")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class BlanketPoController {

    private final ProcurementService procurementService;
    private final VendorService vendorService;
    private final InventoryItemRepository inventoryRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("bpoPage", procurementService.getAllBlanketPos(page, size, q));
        model.addAttribute("q", q);
        model.addAttribute("currentPage", page);
        return "scm/blanket-po-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        BlanketPurchaseOrder bpo = new BlanketPurchaseOrder();
        bpo.getItems().add(new BlanketPoItem());
        model.addAttribute("bpo", bpo);
        model.addAttribute("vendors", vendorService.getActive());
        model.addAttribute("inventoryItems", inventoryRepo.findByActiveTrueOrderByNameAsc());
        return "scm/blanket-po-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("bpo", procurementService.getBlanketPoById(id));
        return "scm/blanket-po-detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("bpo", procurementService.getBlanketPoById(id));
        model.addAttribute("vendors", vendorService.getActive());
        model.addAttribute("inventoryItems", inventoryRepo.findByActiveTrueOrderByNameAsc());
        return "scm/blanket-po-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute BlanketPurchaseOrder bpo, RedirectAttributes ra) {
        try {
            procurementService.saveBlanketPo(bpo);
            ra.addFlashAttribute("success", "Blanket PO saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/blanket-po";
    }
}
