package com.levanto.flooring.controller;

import com.levanto.flooring.dto.*;
import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.*;
import com.levanto.flooring.repository.AppUserRepository;
import com.levanto.flooring.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller @RequestMapping("/admin/billing") @RequiredArgsConstructor
public class BillingController {

    private final BillingService    billingService;
    private final CustomerService   customerService;
    private final InventoryService  inventoryService;
    private final PdfService        pdfService;
    private final AppUserRepository userRepo;

    // ── List ─────────────────────────────────────────────────────────────────
    @GetMapping
    public String list(@RequestParam(defaultValue="0") int page,
                       @RequestParam(defaultValue="15") int size,
                       @RequestParam(required=false) String status,
                       @RequestParam(required=false) String search,
                       Model m) {
        TransactionStatus st = parseStatus(status);
        m.addAttribute("txPage",      billingService.getAll(page, size, st, search));
        m.addAttribute("search",      search);
        m.addAttribute("statusFilter",status);
        m.addAttribute("currentPage", page);
        m.addAttribute("TransactionStatus", TransactionStatus.class);
        m.addAttribute("PaymentStatus",     PaymentStatus.class);
        return "billing/list";
    }

    // ── New form ─────────────────────────────────────────────────────────────
    @GetMapping("/new")
    public String newForm(@RequestParam(defaultValue="QUOTATION") String type, Model m) {
        m.addAttribute("customers", customerService.getAll(0, 500, null).getContent());
        m.addAttribute("products",  inventoryService.getAll());
        m.addAttribute("invoiceType", type);
        m.addAttribute("ItemType",  com.levanto.flooring.enums.ItemType.class);
        return "billing/form";
    }

    // ── Create Quotation (REST) ──────────────────────────────────────────────
    @PostMapping("/quotation") @ResponseBody
    public ResponseEntity<ApiResponse<?>> createQuotation(
            @Valid @RequestBody TransactionRequest req, Authentication auth) {
        AppUser user = resolveUser(auth);
        Transaction tx = billingService.createQuotation(req, user);
        return ResponseEntity.ok(ApiResponse.ok("Quotation created: " + tx.getQuotationNumber(), tx.getId()));
    }

    // ── Create Final Bill (REST) ─────────────────────────────────────────────
    @PostMapping("/finalbill") @ResponseBody
    public ResponseEntity<ApiResponse<?>> createBill(
            @Valid @RequestBody TransactionRequest req, Authentication auth) {
        AppUser user = resolveUser(auth);
        Transaction tx = billingService.createFinalBill(req, user);
        return ResponseEntity.ok(ApiResponse.ok("Invoice created: " + tx.getInvoiceNumber(), tx.getId()));
    }

    // ── Convert quotation → bill ─────────────────────────────────────────────
    @PostMapping("/{id}/convert")
    public String convert(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Transaction tx = billingService.convertToFinalBill(id);
            ra.addFlashAttribute("success", "Converted to Final Bill: " + tx.getInvoiceNumber());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Conversion failed: " + e.getMessage());
        }
        return "redirect:/admin/billing/" + id;
    }

    // ── Update payment ───────────────────────────────────────────────────────
    @PostMapping("/{id}/payment") @ResponseBody
    public ResponseEntity<ApiResponse<?>> updatePayment(
            @PathVariable Long id,
            @RequestParam String paymentStatus,
            @RequestParam(required=false) BigDecimal amountPaid) {
        PaymentStatus ps = PaymentStatus.valueOf(paymentStatus);
        billingService.updatePayment(id, ps, amountPaid);
        return ResponseEntity.ok(ApiResponse.ok("Payment updated."));
    }

    // ── View detail ──────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model m) {
        m.addAttribute("tx",                billingService.getById(id));
        m.addAttribute("TransactionStatus", TransactionStatus.class);
        m.addAttribute("PaymentStatus",     PaymentStatus.class);
        return "billing/view";
    }

    // ── Download PDF ─────────────────────────────────────────────────────────
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        Transaction tx  = billingService.getById(id);
        byte[]      pdf = pdfService.generate(tx);
        String      fn  = (tx.getStatus() == TransactionStatus.FINAL_BILL
            ? tx.getInvoiceNumber() : tx.getQuotationNumber())
            + "_" + tx.getCustomer().getName().replaceAll("\\s+", "_") + ".pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
            .body(pdf);
    }

    // ── Delete quotation ─────────────────────────────────────────────────────
    @DeleteMapping("/{id}") @ResponseBody
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        billingService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted."));
    }

    // ── AJAX helpers ─────────────────────────────────────────────────────────
    @GetMapping("/api/customers/search") @ResponseBody
    public ResponseEntity<?> searchCustomers(@RequestParam String q) {
        return ResponseEntity.ok(customerService.quickSearch(q));
    }

    @GetMapping("/api/items/search") @ResponseBody
    public ResponseEntity<?> searchItems(@RequestParam String q) {
        return ResponseEntity.ok(inventoryService.searchActive(q));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private TransactionStatus parseStatus(String s) {
        if (s == null || s.isBlank()) return null;
        try { return TransactionStatus.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }

    private AppUser resolveUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName()).orElse(null);
    }
}
