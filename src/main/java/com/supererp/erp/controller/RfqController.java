package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.enums.RfqStatus;
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

import java.util.ArrayList;

@Controller
@RequestMapping("/admin/scm/rfq")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class RfqController {

    private final ProcurementService procurementService;
    private final VendorService vendorService;
    private final InventoryItemRepository inventoryRepo;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("rfqPage", procurementService.getAllRfqs(page, size, q));
        model.addAttribute("q", q);
        model.addAttribute("currentPage", page);
        return "scm/rfq-list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        RequestForQuotation rfq = new RequestForQuotation();
        rfq.getItems().add(new RfqItem());
        model.addAttribute("rfq", rfq);
        model.addAttribute("vendors", vendorService.getActive());
        model.addAttribute("inventoryItems", inventoryRepo.findByActiveTrueOrderByNameAsc());
        return "scm/rfq-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("rfq", procurementService.getRfqById(id));
        model.addAttribute("vendors", vendorService.getActive());
        return "scm/rfq-detail";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("rfq", procurementService.getRfqById(id));
        model.addAttribute("vendors", vendorService.getActive());
        model.addAttribute("inventoryItems", inventoryRepo.findByActiveTrueOrderByNameAsc());
        return "scm/rfq-form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute RequestForQuotation rfq, RedirectAttributes ra) {
        try {
            procurementService.saveRfq(rfq);
            ra.addFlashAttribute("success", "RFQ saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/rfq";
    }

    @PostMapping("/{id}/status")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> updateStatus(@PathVariable Long id,
                                                        @RequestParam RfqStatus status) {
        try {
            procurementService.updateRfqStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("RFQ status updated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/award")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> award(@PathVariable Long id,
                                                 @RequestParam Long vendorResponseId) {
        try {
            procurementService.awardRfq(id, vendorResponseId);
            return ResponseEntity.ok(ApiResponse.ok("RFQ awarded successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/response")
    public String addResponse(@PathVariable Long id,
                               @ModelAttribute RfqVendorResponse response,
                               RedirectAttributes ra) {
        try {
            procurementService.addVendorResponse(id, response);
            ra.addFlashAttribute("success", "Vendor response recorded.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/rfq/" + id;
    }
}
