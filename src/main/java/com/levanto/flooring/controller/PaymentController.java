package com.levanto.flooring.controller;

import com.levanto.flooring.dto.ApiResponse;
import com.levanto.flooring.entity.IncomeTransaction;
import com.levanto.flooring.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ── Income Ledger page ────────────────────────────────────────────────────
    @GetMapping
    public String ledger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model m) {
        LocalDate f = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate t = to   != null ? to   : LocalDate.now();
        m.addAttribute("ledgerPage",    paymentService.getLedger(page, size, f, t));
        m.addAttribute("totalIncome",   paymentService.monthlyIncome(f, t));
        m.addAttribute("from", f);
        m.addAttribute("to",   t);
        m.addAttribute("currentPage",   page);
        return "finance/ledger";
    }

    // ── Mark Fully Paid (REST) ────────────────────────────────────────────────
    @PostMapping("/transactions/{id}/pay-full") @ResponseBody
    public ResponseEntity<ApiResponse<?>> payFull(@PathVariable Long id) {
        try {
            IncomeTransaction income = paymentService.markFullyPaid(id);
            return ResponseEntity.ok(ApiResponse.ok("Invoice marked as Fully Paid.", income.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Record Partial Payment (REST) ─────────────────────────────────────────
    @PostMapping("/transactions/{id}/pay-partial") @ResponseBody
    public ResponseEntity<ApiResponse<?>> payPartial(
            @PathVariable Long id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String title) {
        try {
            IncomeTransaction income = paymentService.recordPartialPayment(id, amount, title);
            return ResponseEntity.ok(ApiResponse.ok("Partial payment recorded.", income.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Payment history for one invoice (REST) ────────────────────────────────
    @GetMapping("/transactions/{id}/history") @ResponseBody
    public ResponseEntity<?> history(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentsForTransaction(id));
    }
}
