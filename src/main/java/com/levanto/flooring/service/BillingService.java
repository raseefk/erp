package com.levanto.flooring.service;

import com.levanto.flooring.config.CompanyProperties;
import com.levanto.flooring.dto.TransactionRequest;
import com.levanto.flooring.entity.*;
import com.levanto.flooring.enums.*;
import com.levanto.flooring.repository.*;
import com.levanto.flooring.util.NumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class BillingService {

    private final TransactionRepository txRepo;
    private final CustomerRepository    customerRepo;
    private final InventoryItemRepository itemRepo;
    private final NumberGenerator       numGen;
    private final CompanyProperties     company;

    // ── Create Quotation (no stock deduction) ─────────────────────────────────
    @Transactional
    public Transaction createQuotation(TransactionRequest req, AppUser createdBy) {
        Transaction tx = buildTransaction(req, TransactionStatus.QUOTATION, createdBy);
        tx.setQuotationNumber(numGen.nextQuotationNumber());
        return txRepo.save(tx);
    }

    // ── Create Final Bill directly (with stock deduction) ─────────────────────
    @Transactional
    public Transaction createFinalBill(TransactionRequest req, AppUser createdBy) {
        Transaction tx = buildTransaction(req, TransactionStatus.FINAL_BILL, createdBy);
        tx.setInvoiceNumber(numGen.nextInvoiceNumber());
        tx.setConvertedAt(LocalDateTime.now());
        deductStock(tx.getItems());
        return txRepo.save(tx);
    }

    // ── Convert Quotation → Final Bill (stock deducted only here) ────────────
    @Transactional
    public Transaction convertToFinalBill(Long quotationId) {
        Transaction tx = getById(quotationId);
        if (tx.getStatus() != TransactionStatus.QUOTATION)
            throw new IllegalStateException("Transaction is already a Final Bill.");

        deductStock(tx.getItems());
        tx.setStatus(TransactionStatus.FINAL_BILL);
        tx.setInvoiceNumber(numGen.nextInvoiceNumber());
        tx.setConvertedAt(LocalDateTime.now());
        return txRepo.save(tx);
    }

    // ── Update payment status ─────────────────────────────────────────────────
    @Transactional
    public Transaction updatePayment(Long id, PaymentStatus paymentStatus, BigDecimal amountPaid) {
        Transaction tx = getById(id);
        tx.setPaymentStatus(paymentStatus);
        if (amountPaid != null) tx.setAmountPaid(amountPaid);
        return txRepo.save(tx);
    }

    // ── Read ──────────────────────────────────────────────────────────────────
    public Transaction getById(Long id) {
        return txRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public Page<Transaction> getAll(int page, int size, TransactionStatus status, String search) {
        Pageable pg = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank())
            return txRepo.search(search.trim(), status, pg);
        if (status != null)
            return txRepo.findByStatusOrderByCreatedAtDesc(status, pg);
        return txRepo.findAll(pg);
    }

    @Transactional
    public void delete(Long id) {
        Transaction tx = getById(id);
        if (tx.getStatus() == TransactionStatus.FINAL_BILL)
            throw new IllegalStateException("Final Bills cannot be deleted.");
        txRepo.delete(tx);
    }

    public long count(TransactionStatus s) { return txRepo.countByStatus(s); }
    public long countPayment(PaymentStatus s) { return txRepo.countByPaymentStatus(s); }

    public BigDecimal monthlyIncome() {
        LocalDateTime from = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime to   = LocalDateTime.now();
        return txRepo.sumGrandTotalByStatusAndDateRange(TransactionStatus.FINAL_BILL, from, to);
    }

    // ── Core builder ──────────────────────────────────────────────────────────
    private Transaction buildTransaction(TransactionRequest req,
                                         TransactionStatus status,
                                         AppUser createdBy) {
        Customer customer = customerRepo.findById(req.getCustomerId())
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Transaction tx = Transaction.builder()
            .status(status)
            .customer(customer)
            .gstEnabled(req.isGstEnabled())
            .taxAllItems(req.isTaxAllItems())
            .gstType(GstType.valueOf(req.getGstType() != null ? req.getGstType() : "LOCAL"))
            .notes(req.getNotes())
            .createdBy(createdBy)
            .items(new ArrayList<>())
            .build();

        BigDecimal subtotal   = BigDecimal.ZERO;
        BigDecimal totalCgst  = BigDecimal.ZERO;
        BigDecimal totalSgst  = BigDecimal.ZERO;
        BigDecimal totalIgst  = BigDecimal.ZERO;

        for (TransactionRequest.LineItemRequest lr : req.getItems()) {
            TransactionItem item = new TransactionItem();
            item.setTransaction(tx);
            item.setItemType(lr.getItemType());
            item.setUnit(lr.getUnit() != null ? lr.getUnit() : "SQF");

            // Resolve inventory item
            if (lr.getInventoryItemId() != null) {
                InventoryItem inv = itemRepo.findById(lr.getInventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + lr.getInventoryItemId()));
                item.setInventoryItem(inv);
                item.setDescription(lr.getDescription() != null && !lr.getDescription().isBlank()
                    ? lr.getDescription() : inv.getName());
                item.setHsnSacCode(lr.getHsnSacCode() != null ? lr.getHsnSacCode() : inv.getHsnSacCode());
            } else {
                item.setDescription(lr.getDescription() != null ? lr.getDescription() : "Custom Item");
                item.setHsnSacCode(lr.getHsnSacCode());
            }

            // Measurement: squareFeet takes priority, else quantity
            BigDecimal effectiveQty;
            if (lr.getSquareFeet() != null && lr.getSquareFeet().compareTo(BigDecimal.ZERO) > 0) {
                item.setSquareFeet(lr.getSquareFeet());
                item.setQuantity(lr.getQuantity());
                effectiveQty = lr.getSquareFeet();
            } else {
                item.setQuantity(lr.getQuantity());
                item.setSquareFeet(BigDecimal.ZERO);
                effectiveQty = lr.getQuantity();
            }

            item.setRatePerUnit(lr.getRatePerUnit());

            // ── Smart GST Engine ────────────────────────────────────────────
            BigDecimal gstPct = BigDecimal.ZERO;
            boolean applyGst = false;
            if (lr.getItemType() == ItemType.SERVICE && req.isGstEnabled()) {
                applyGst = true;
            } else if (lr.getItemType() == ItemType.PRODUCT && req.isTaxAllItems()) {
                applyGst = true;
            }

            if (applyGst) {
                gstPct = lr.getGstPercent() != null ? lr.getGstPercent()
                    : BigDecimal.valueOf(18); // Default fallback if not provided
            }
            item.setGstPercent(gstPct);

            // Calculate amounts
            BigDecimal base = lr.getRatePerUnit()
                .multiply(effectiveQty)
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal gstTotal = base.multiply(gstPct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (tx.getGstType() == GstType.IGST) {
                item.setIgstAmount(gstTotal);
                item.setCgstAmount(BigDecimal.ZERO);
                item.setSgstAmount(BigDecimal.ZERO);
                totalIgst = totalIgst.add(gstTotal);
            } else {
                BigDecimal cgst = gstTotal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                BigDecimal sgst = gstTotal.subtract(cgst); // avoids rounding gap
                item.setCgstAmount(cgst);
                item.setSgstAmount(sgst);
                item.setIgstAmount(BigDecimal.ZERO);
                totalCgst = totalCgst.add(cgst);
                totalSgst = totalSgst.add(sgst);
            }

            item.setBaseAmount(base);
            item.setTotalAmount(base.add(gstTotal));
            subtotal = subtotal.add(base);
            tx.getItems().add(item);
        }

        tx.setSubtotal(subtotal);
        tx.setTotalCgst(totalCgst);
        tx.setTotalSgst(totalSgst);
        tx.setTotalIgst(totalIgst);
        tx.setGrandTotal(subtotal.add(totalCgst).add(totalSgst).add(totalIgst));
        tx.setAmountPaid(BigDecimal.ZERO);
        tx.setPaymentStatus(PaymentStatus.PENDING);

        return tx;
    }

    // ── Stock deduction (only PRODUCT items) ──────────────────────────────────
    private void deductStock(List<TransactionItem> items) {
        for (TransactionItem item : items) {
            if (item.getItemType() == ItemType.PRODUCT && item.getInventoryItem() != null) {
                InventoryItem inv = item.getInventoryItem();
                // Use ceiling of squareFeet/quantity as integer stock units
                int qty = item.getQuantity().setScale(0, RoundingMode.CEILING).intValue();
                if (inv.getStockQuantity() < qty) {
                    throw new IllegalStateException(
                        "Insufficient stock for '" + inv.getName() +
                        "'. Available: " + inv.getStockQuantity() + " | Required: " + qty);
                }
                inv.setStockQuantity(inv.getStockQuantity() - qty);
                itemRepo.save(inv);
                log.info("Stock deducted: {} → -{} (remaining: {})", inv.getName(), qty, inv.getStockQuantity());
            }
        }
    }
}
