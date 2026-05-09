package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.AdvancePayment;
import com.supererp.erp.service.AdvancePaymentService;
import com.supererp.erp.service.PdfService;
import com.supererp.erp.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/advances")
@RequiredArgsConstructor
@com.supererp.erp.rbac.annotation.RequiresFeature("ADVANCE_PAYMENTS")
public class AdvancePaymentController {

    private final AdvancePaymentService advanceService;
    private final ProjectService projectService;
    private final PdfService pdfService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model m) {
        m.addAttribute("advancePage", advanceService.getAdvances(page, size));
        m.addAttribute("currentPage", page);
        return "sales/advance-payments/list";
    }

    @GetMapping("/create")
    public String createForm(Model m) {
        m.addAttribute("projects", projectService.getActive());
        return "sales/advance-payments/create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String paymentFrom,
                         @RequestParam(required = false) Long projectId,
                         @RequestParam BigDecimal amount,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         RedirectAttributes ra) {
        try {
            AdvancePayment advance = advanceService.createAdvance(paymentFrom, projectId, amount, description, date);
            ra.addFlashAttribute("success", "Advance Payment Recorded: " + advance.getAdvanceNumber());
            return "redirect:/admin/advances";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error recording advance: " + e.getMessage());
            return "redirect:/admin/advances/create";
        }
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> receiptPdf(@PathVariable Long id) {
        AdvancePayment advance = advanceService.getAdvanceById(id);
        byte[] pdf = pdfService.generateAdvanceReceipt(advance);
        String fn = advance.getAdvanceNumber() + "_Receipt.pdf";
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fn + "\"")
            .body(pdf);
    }
}
