package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.VendorInvoice;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.service.ProcurementService;
import com.supererp.erp.service.ScmService;
import com.supererp.erp.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vendor Invoice controller — 3-way matching workflow.
 */
@Controller
@RequestMapping("/admin/scm/invoices")
@RequiredArgsConstructor
@RequiresFeature("SCM")
public class VendorInvoiceController {

    private final ProcurementService procurementService;
    private final ScmService scmService;
    private final VendorService vendorService;
    private final com.supererp.erp.service.FileStorageService fileStorageService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String q,
                       Model model) {
        model.addAttribute("invoicePage", procurementService.getAllInvoices(page, size, q));
        model.addAttribute("q", q);
        model.addAttribute("currentPage", page);
        return "scm/invoice-list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long poId, Model model) {
        VendorInvoice invoice = new VendorInvoice();
        if (poId != null) {
            var po = scmService.getPOById(poId);
            invoice.setPurchaseOrder(po);
            invoice.setVendor(po.getVendor());
            invoice.setInvoiceAmount(po.getTotalAmount());
            invoice.setTotalAmount(po.getTotalAmount());
            model.addAttribute("selectedPoGrns", procurementService.getGrnsForPo(poId));
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("purchaseOrders", scmService.getAllOrderedPos());
        model.addAttribute("vendors", vendorService.getActive());
        return "scm/invoice-form";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", procurementService.getInvoiceById(id));
        return "scm/invoice-detail";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute VendorInvoice invoice,
                       @RequestParam(value = "invoiceFile", required = false) MultipartFile file,
                       RedirectAttributes ra) {
        try {
            if (file != null && !file.isEmpty()) {
                String filePath = fileStorageService.store(file, "vendor-invoices");
                invoice.setFilePath(filePath);
            }
            procurementService.submitVendorInvoice(invoice);
            ra.addFlashAttribute("success", "Invoice submitted. 3-way match completed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/admin/scm/invoices";
    }

    @PostMapping("/{id}/mark-paid")
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> markPaid(@PathVariable Long id,
                                                    @RequestParam java.math.BigDecimal amount) {
        try {
            procurementService.recordInvoicePayment(id, amount);
            return ResponseEntity.ok(ApiResponse.ok("Payment recorded."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
