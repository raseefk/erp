package com.levanto.flooring.service;

import com.levanto.flooring.entity.IncomeTransaction;
import com.levanto.flooring.entity.Transaction;
import com.levanto.flooring.enums.PaymentStatus;
import com.levanto.flooring.enums.TransactionStatus;
import com.levanto.flooring.repository.IncomeTransactionRepository;
import com.levanto.flooring.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository       txRepo;
    private final IncomeTransactionRepository incomeRepo;

    // ── Mark Fully Paid ───────────────────────────────────────────────────────
    /**
     * Marks a FINAL_BILL as FULLY_PAID.
     * Inserts one IncomeTransaction for the remaining balance.
     */
    @Transactional
    public IncomeTransaction markFullyPaid(Long transactionId) {
        Transaction tx = getInvoice(transactionId);
        assertNotFullyPaid(tx);

        BigDecimal alreadyPaid = incomeRepo.sumByTransactionId(transactionId);
        BigDecimal remaining   = tx.getGrandTotal().subtract(alreadyPaid).setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice is already fully settled.");
        }

        IncomeTransaction income = buildIncome(tx, "Full Payment", remaining, alreadyPaid.add(remaining), tx.getGrandTotal());

        tx.setAmountPaid(tx.getGrandTotal());
        tx.setPaymentStatus(PaymentStatus.PAID);
        txRepo.save(tx);

        return incomeRepo.save(income);
    }

    // ── Record Partial Payment ────────────────────────────────────────────────
    /**
     * Records a partial payment.
     * If the cumulative total reaches grandTotal → auto-transitions to FULLY_PAID.
     */
    @Transactional
    public IncomeTransaction recordPartialPayment(Long transactionId, BigDecimal payAmount, String title) {
        Transaction tx = getInvoice(transactionId);
        assertNotFullyPaid(tx);

        if (payAmount == null || payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }

        BigDecimal alreadyPaid = incomeRepo.sumByTransactionId(transactionId);
        BigDecimal newTotal    = alreadyPaid.add(payAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal  = tx.getGrandTotal();

        if (newTotal.compareTo(grandTotal) > 0) {
            throw new IllegalArgumentException(
                "Payment of ₹" + payAmount + " exceeds remaining balance of ₹" +
                grandTotal.subtract(alreadyPaid).setScale(2, RoundingMode.HALF_UP)
            );
        }

        String effectiveTitle = (title != null && !title.isBlank()) ? title : "Partial Payment";
        IncomeTransaction income = buildIncome(tx, effectiveTitle, payAmount, newTotal, grandTotal);

        // Auto-transition if balance is now zero
        if (newTotal.compareTo(grandTotal) == 0) {
            tx.setPaymentStatus(PaymentStatus.PAID);
            log.info("Auto-transitioned Invoice {} to FULLY_PAID", tx.getInvoiceNumber());
        } else {
            tx.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }
        tx.setAmountPaid(newTotal);
        txRepo.save(tx);

        return incomeRepo.save(income);
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public List<IncomeTransaction> getPaymentsForTransaction(Long transactionId) {
        Transaction tx = txRepo.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        return incomeRepo.findByTransactionOrderByDateDesc(tx);
    }

    public BigDecimal totalReceivedForTransaction(Long transactionId) {
        return incomeRepo.sumByTransactionId(transactionId);
    }

    public Page<IncomeTransaction> getLedger(int page, int size, LocalDate from, LocalDate to) {
        Pageable pg = PageRequest.of(page, size, Sort.by("date").descending());
        if (from != null && to != null) return incomeRepo.findByDateRange(from, to, pg);
        return incomeRepo.findAllByOrderByDateDesc(pg);
    }

    public BigDecimal monthlyIncome(LocalDate from, LocalDate to) {
        return incomeRepo.sumByDateRange(from, to);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Transaction getInvoice(Long id) {
        Transaction tx = txRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        if (tx.getStatus() != TransactionStatus.FINAL_BILL) {
            throw new IllegalStateException("Payments can only be recorded against a Final Bill.");
        }
        return tx;
    }

    private void assertNotFullyPaid(Transaction tx) {
        if (tx.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Invoice " + tx.getInvoiceNumber() + " is already fully paid.");
        }
    }

    private IncomeTransaction buildIncome(Transaction tx, String title,
                                          BigDecimal amount, BigDecimal totalReceived,
                                          BigDecimal grandTotal) {
        BigDecimal pending = grandTotal.subtract(totalReceived).setScale(2, RoundingMode.HALF_UP);
        if (pending.compareTo(BigDecimal.ZERO) < 0) pending = BigDecimal.ZERO;

        String description = String.format(
            "Bill Amount: ₹%s, Total Received: ₹%s, Pending Amount: ₹%s",
            grandTotal.setScale(2, RoundingMode.HALF_UP),
            totalReceived.setScale(2, RoundingMode.HALF_UP),
            pending
        );

        return IncomeTransaction.builder()
            .transaction(tx)
            .inventoryNumber(tx.getInvoiceNumber())
            .title(title)
            .amount(amount.setScale(2, RoundingMode.HALF_UP))
            .description(description)
            .date(LocalDate.now())
            .build();
    }
}
