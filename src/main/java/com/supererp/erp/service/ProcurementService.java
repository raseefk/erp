package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.rbac.annotation.AuditAction;
import com.supererp.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Core Procurement / SCM service.
 * Handles GRN, 3-way matching, Vendor Rating, RFQ, Blanket POs,
 * Approval Matrix, and Landed Cost Allocation.
 */
@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final GoodsReceiptNoteRepository grnRepo;
    private final VendorInvoiceRepository invoiceRepo;
    private final VendorRatingRepository ratingRepo;
    private final RequestForQuotationRepository rfqRepo;
    private final BlanketPurchaseOrderRepository bpoRepo;
    private final ProcurementApprovalRepository approvalRepo;
    private final LandedCostAllocationRepository landedCostRepo;
    private final PurchaseOrderRepository poRepo;
    private final InventoryItemRepository inventoryRepo;
    private final VendorRepository vendorRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // GRN — Goods Receipt Note
    // ─────────────────────────────────────────────────────────────────────────

    public Page<GoodsReceiptNote> getAllGrns(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return grnRepo.searchGrns(q != null ? q.trim() : null, pageable);
    }

    public GoodsReceiptNote getGrnById(Long id) {
        return grnRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + id));
    }

    @Transactional
    @AuditAction(value = "GRN_SUBMIT", entityType = "GoodsReceiptNote")
    public GoodsReceiptNote submitGrn(GoodsReceiptNote grn) {
        if (grn.getGrnNumber() == null || grn.getGrnNumber().isBlank()) {
            grn.setGrnNumber(generateGrnNumber());
        }
        if (grn.getReceivedDate() == null) grn.setReceivedDate(LocalDate.now());

        // Calculate totals and link items
        BigDecimal total = BigDecimal.ZERO;
        for (GrnItem item : grn.getItems()) {
            item.setGrn(grn);
            item.setTotalValue(item.getUnitPrice().multiply(item.getAcceptedQuantity()));
            total = total.add(item.getTotalValue());
        }
        grn.setTotalReceivedValue(total);
        grn.setStatus(GrnStatus.SUBMITTED);
        return grnRepo.save(grn);
    }

    @Transactional
    @AuditAction(value = "GRN_ACCEPT", entityType = "GoodsReceiptNote")
    public GoodsReceiptNote acceptGrn(Long grnId, String remarks) {
        GoodsReceiptNote grn = getGrnById(grnId);
        if (grn.getStatus() != GrnStatus.SUBMITTED) {
            throw new IllegalStateException("GRN must be in SUBMITTED status to accept.");
        }
        grn.setStatus(GrnStatus.ACCEPTED);
        grn.setRemarks(remarks);

        // Update PO status and inventory stock
        grn.getItems().forEach(item -> {
            if (item.getInventoryItem() != null && item.getAcceptedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                InventoryItem inv = item.getInventoryItem();
                inv.setStockQuantity(inv.getStockQuantity() + item.getAcceptedQuantity().intValue());
                inventoryRepo.save(inv);
            }
        });

        PurchaseOrder po = grn.getPurchaseOrder();
        po.setStatus(PurchaseOrderStatus.RECEIVED);
        po.setActualDeliveryDate(LocalDate.now());
        poRepo.save(po);

        return grnRepo.save(grn);
    }

    @Transactional
    @AuditAction(value = "GRN_REJECT", entityType = "GoodsReceiptNote")
    public GoodsReceiptNote rejectGrn(Long grnId, String remarks) {
        GoodsReceiptNote grn = getGrnById(grnId);
        grn.setStatus(GrnStatus.REJECTED);
        grn.setRemarks(remarks);
        return grnRepo.save(grn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vendor Invoice & 3-Way Match
    // ─────────────────────────────────────────────────────────────────────────

    public Page<VendorInvoice> getAllInvoices(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return invoiceRepo.searchInvoices(q != null ? q.trim() : null, pageable);
    }

    public VendorInvoice getInvoiceById(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    @Transactional
    @AuditAction(value = "VENDOR_INVOICE_SUBMIT", entityType = "VendorInvoice")
    public VendorInvoice submitVendorInvoice(VendorInvoice invoice) {
        // Perform 3-way match
        performThreeWayMatch(invoice);
        return invoiceRepo.save(invoice);
    }

    private void performThreeWayMatch(VendorInvoice invoice) {
        PurchaseOrder po = invoice.getPurchaseOrder();
        BigDecimal poTotal = po.getTotalAmount();
        BigDecimal invoiceTotal = invoice.getTotalAmount();

        // Tolerance: 1% or ₹100, whichever is higher
        BigDecimal tolerance = poTotal.multiply(new BigDecimal("0.01"))
                .max(new BigDecimal("100"));

        boolean poMatch = invoiceTotal.subtract(poTotal).abs().compareTo(tolerance) <= 0;
        invoice.setPoAmountMatch(poMatch);

        // GRN match
        if (invoice.getGrn() != null) {
            BigDecimal grnTotal = invoice.getGrn().getTotalReceivedValue();
            boolean grnMatch = invoiceTotal.subtract(grnTotal).abs().compareTo(tolerance) <= 0;
            invoice.setGrnAmountMatch(grnMatch);

            if (poMatch && grnMatch) {
                invoice.setMatchStatus("FULL_MATCH");
                invoice.setMatchNotes("PO, GRN and Invoice amounts match within tolerance.");
            } else if (poMatch || grnMatch) {
                invoice.setMatchStatus("PARTIAL_MATCH");
                invoice.setMatchNotes(buildMatchNotes(poMatch, grnMatch, poTotal, grnTotal, invoiceTotal));
            } else {
                invoice.setMatchStatus("DISCREPANCY");
                invoice.setMatchNotes(buildMatchNotes(poMatch, grnMatch, poTotal, grnTotal, invoiceTotal));
            }
        } else {
            // 2-way match only (PO vs Invoice)
            invoice.setGrnAmountMatch(null);
            if (poMatch) {
                invoice.setMatchStatus("PARTIAL_MATCH");
                invoice.setMatchNotes("PO amount matches invoice. No GRN linked for 3-way match.");
            } else {
                invoice.setMatchStatus("DISCREPANCY");
                invoice.setMatchNotes("Invoice ₹" + invoiceTotal + " differs from PO ₹" + poTotal + " beyond tolerance.");
            }
        }
    }

    private String buildMatchNotes(boolean poMatch, boolean grnMatch, BigDecimal poTotal,
                                    BigDecimal grnTotal, BigDecimal invoiceTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice: ₹").append(invoiceTotal).append(" | ");
        sb.append("PO: ₹").append(poTotal).append(" (").append(poMatch ? "✓ Match" : "✗ Mismatch").append(") | ");
        sb.append("GRN: ₹").append(grnTotal).append(" (").append(grnMatch ? "✓ Match" : "✗ Mismatch").append(")");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Vendor Rating / Scorecard
    // ─────────────────────────────────────────────────────────────────────────

    public Page<VendorRating> getAllRatings(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("ratingDate").descending());
        return ratingRepo.searchRatings(q != null ? q.trim() : null, pageable);
    }

    public List<VendorRating> getRatingsForVendor(Long vendorId) {
        return ratingRepo.findByVendorIdOrderByRatingDateDesc(vendorId);
    }

    public BigDecimal getVendorAverageScore(Long vendorId) {
        BigDecimal avg = ratingRepo.findAverageScoreByVendorId(vendorId);
        return avg != null ? avg.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public List<Object[]> getVendorScorecardSummary() {
        return ratingRepo.findVendorScorecardSummary();
    }

    @Transactional
    @AuditAction(value = "VENDOR_RATING_SAVE", entityType = "VendorRating")
    public VendorRating saveVendorRating(VendorRating rating) {
        // Calculate overall score as average of 4 dimensions
        BigDecimal sum = rating.getQualityScore()
                .add(rating.getDeliveryScore())
                .add(rating.getPriceScore())
                .add(rating.getServiceScore());
        rating.setOverallScore(sum.divide(new BigDecimal("4"), 2, RoundingMode.HALF_UP));
        if (rating.getRatingDate() == null) rating.setRatingDate(LocalDate.now());
        return ratingRepo.save(rating);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RFQ — Request for Quotation
    // ─────────────────────────────────────────────────────────────────────────

    public Page<RequestForQuotation> getAllRfqs(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return rfqRepo.searchRfqs(q != null ? q.trim() : null, pageable);
    }

    public RequestForQuotation getRfqById(Long id) {
        return rfqRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RFQ not found: " + id));
    }

    @Transactional
    @AuditAction(value = "RFQ_SAVE", entityType = "RequestForQuotation")
    public RequestForQuotation saveRfq(RequestForQuotation rfq) {
        if (rfq.getRfqNumber() == null || rfq.getRfqNumber().isBlank()) {
            rfq.setRfqNumber(generateRfqNumber());
        }
        if (rfq.getIssueDate() == null) rfq.setIssueDate(LocalDate.now());
        rfq.getItems().forEach(item -> item.setRfq(rfq));
        return rfqRepo.save(rfq);
    }

    @Transactional
    @AuditAction(value = "RFQ_STATUS_CHANGE", entityType = "RequestForQuotation")
    public RequestForQuotation updateRfqStatus(Long rfqId, RfqStatus newStatus) {
        RequestForQuotation rfq = getRfqById(rfqId);
        rfq.setStatus(newStatus);
        return rfqRepo.save(rfq);
    }

    @Transactional
    @AuditAction(value = "RFQ_VENDOR_RESPONSE_SAVE", entityType = "RfqVendorResponse")
    public RequestForQuotation addVendorResponse(Long rfqId, RfqVendorResponse response) {
        RequestForQuotation rfq = getRfqById(rfqId);
        response.setRfq(rfq);
        response.getResponseItems().forEach(ri -> ri.setVendorResponse(response));

        // Calculate total
        BigDecimal total = response.getResponseItems().stream()
                .map(ri -> {
                    ri.setTotalPrice(ri.getUnitPrice().multiply(ri.getRfqItem().getQuantity()));
                    return ri.getTotalPrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalQuotedAmount(total);
        response.setResponseDate(LocalDate.now());

        rfq.getVendorResponses().add(response);
        if (rfq.getStatus() == RfqStatus.SENT) {
            rfq.setStatus(RfqStatus.RESPONSES_RECEIVED);
        }
        return rfqRepo.save(rfq);
    }

    @Transactional
    @AuditAction(value = "RFQ_AWARD", entityType = "RequestForQuotation")
    public RequestForQuotation awardRfq(Long rfqId, Long vendorResponseId) {
        RequestForQuotation rfq = getRfqById(rfqId);
        rfq.getVendorResponses().forEach(r -> r.setAwarded(r.getId().equals(vendorResponseId)));
        rfq.setStatus(RfqStatus.AWARDED);
        rfq.getVendorResponses().stream()
                .filter(r -> r.getId().equals(vendorResponseId))
                .findFirst()
                .ifPresent(r -> rfq.setAwardedVendorId(r.getVendor().getId()));
        return rfqRepo.save(rfq);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Blanket PO / Rate Contracts
    // ─────────────────────────────────────────────────────────────────────────

    public Page<BlanketPurchaseOrder> getAllBlanketPos(int page, int size, String q) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return bpoRepo.searchBpos(q != null ? q.trim() : null, pageable);
    }

    public BlanketPurchaseOrder getBlanketPoById(Long id) {
        return bpoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blanket PO not found: " + id));
    }

    @Transactional
    @AuditAction(value = "BLANKET_PO_SAVE", entityType = "BlanketPurchaseOrder")
    public BlanketPurchaseOrder saveBlanketPo(BlanketPurchaseOrder bpo) {
        if (bpo.getBpoNumber() == null || bpo.getBpoNumber().isBlank()) {
            bpo.setBpoNumber(generateBpoNumber());
        }
        bpo.getItems().forEach(item -> item.setBlanketPo(bpo));
        return bpoRepo.save(bpo);
    }

    @Transactional
    public void expireOldBlanketPos() {
        List<BlanketPurchaseOrder> expired = bpoRepo.findExpiredActiveBpos(LocalDate.now());
        expired.forEach(bpo -> bpo.setStatus(BlanketPoStatus.EXPIRED));
        bpoRepo.saveAll(expired);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Procurement Approval Matrix
    // ─────────────────────────────────────────────────────────────────────────

    public List<ProcurementApproval> getApprovalsForPo(Long poId) {
        return approvalRepo.findByPurchaseOrderIdOrderByApprovalLevel(poId);
    }

    public List<ProcurementApproval> getAllPendingApprovals() {
        return approvalRepo.findAllPending();
    }

    @Transactional
    public List<ProcurementApproval> triggerApprovalWorkflow(PurchaseOrder po) {
        // Amount-based escalation matrix
        // L1: ≤ 50,000 — Manager
        // L2: 50,001 – 5,00,000 — Finance Head
        // L3: > 5,00,000 — MD/Director
        List<ProcurementApproval> approvals = new ArrayList<>();
        BigDecimal amount = po.getTotalAmount();

        if (amount.compareTo(new BigDecimal("50000")) <= 0) {
            approvals.add(buildApproval(po, 1, "MANAGER", BigDecimal.ZERO, new BigDecimal("50000")));
        } else if (amount.compareTo(new BigDecimal("500000")) <= 0) {
            approvals.add(buildApproval(po, 1, "MANAGER", BigDecimal.ZERO, new BigDecimal("50000")));
            approvals.add(buildApproval(po, 2, "FINANCE_HEAD", new BigDecimal("50001"), new BigDecimal("500000")));
        } else {
            approvals.add(buildApproval(po, 1, "MANAGER", BigDecimal.ZERO, new BigDecimal("50000")));
            approvals.add(buildApproval(po, 2, "FINANCE_HEAD", new BigDecimal("50001"), new BigDecimal("500000")));
            approvals.add(buildApproval(po, 3, "DIRECTOR", new BigDecimal("500001"), new BigDecimal("99999999")));
        }
        return approvalRepo.saveAll(approvals);
    }

    private ProcurementApproval buildApproval(PurchaseOrder po, int level, String role,
                                               BigDecimal minAmt, BigDecimal maxAmt) {
        return ProcurementApproval.builder()
                .purchaseOrder(po)
                .approvalLevel(level)
                .approverRole(role)
                .approverName(role + " Approval Required")
                .minAmount(minAmt)
                .maxAmount(maxAmt)
                .status(ProcurementApprovalStatus.PENDING)
                .build();
    }

    @Transactional
    @AuditAction(value = "PROCUREMENT_APPROVE", entityType = "ProcurementApproval")
    public ProcurementApproval approveRequest(Long approvalId, String comments, String approverName) {
        ProcurementApproval approval = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
        approval.setStatus(ProcurementApprovalStatus.APPROVED);
        approval.setApprovedAt(java.time.LocalDateTime.now());
        approval.setComments(comments);
        approval.setApproverName(approverName);

        // Check if all approvals for PO are done — update PO status
        Long poId = approval.getPurchaseOrder().getId();
        List<ProcurementApproval> allApprovals = approvalRepo.findByPurchaseOrderIdOrderByApprovalLevel(poId);
        boolean allApproved = allApprovals.stream()
                .allMatch(a -> a.getStatus() == ProcurementApprovalStatus.APPROVED
                        || a.getId().equals(approvalId));
        if (allApproved) {
            PurchaseOrder po = approval.getPurchaseOrder();
            po.setStatus(PurchaseOrderStatus.ORDERED);
            poRepo.save(po);
        }
        return approvalRepo.save(approval);
    }

    @Transactional
    @AuditAction(value = "PROCUREMENT_REJECT", entityType = "ProcurementApproval")
    public ProcurementApproval rejectRequest(Long approvalId, String comments, String approverName) {
        ProcurementApproval approval = approvalRepo.findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));
        approval.setStatus(ProcurementApprovalStatus.REJECTED);
        approval.setApprovedAt(java.time.LocalDateTime.now());
        approval.setComments(comments);
        approval.setApproverName(approverName);

        // Cancel the PO
        PurchaseOrder po = approval.getPurchaseOrder();
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        poRepo.save(po);

        return approvalRepo.save(approval);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Landed Cost
    // ─────────────────────────────────────────────────────────────────────────

    public List<LandedCostAllocation> getLandedCostsForPo(Long poId) {
        return landedCostRepo.findByPurchaseOrderIdOrderByCreatedAt(poId);
    }

    @Transactional
    @AuditAction(value = "LANDED_COST_SAVE", entityType = "LandedCostAllocation")
    public LandedCostAllocation saveLandedCost(LandedCostAllocation cost) {
        return landedCostRepo.save(cost);
    }

    @Transactional
    public void deleteLandedCost(Long id) {
        landedCostRepo.deleteById(id);
    }

    public BigDecimal getTotalLandedCostForPo(Long poId) {
        BigDecimal total = landedCostRepo.sumByPurchaseOrderId(poId);
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Number Generators
    // ─────────────────────────────────────────────────────────────────────────

    private String generateGrnNumber() {
        String prefix = "GRN-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        return prefix + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    private String generateRfqNumber() {
        String prefix = "RFQ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        return prefix + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    private String generateBpoNumber() {
        String prefix = "BPO-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        return prefix + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    /** Helper for controllers */
    public List<VendorInvoice> getInvoicesForPo(Long poId) {
        return invoiceRepo.findByPurchaseOrderIdOrderByCreatedAtDesc(poId);
    }

    /** Helper for controllers — GRNs linked to a PO */
    public List<GoodsReceiptNote> getGrnsForPo(Long poId) {
        return grnRepo.findByPurchaseOrderId(poId);
    }

    @Transactional
    @AuditAction(value = "VENDOR_INVOICE_PAYMENT", entityType = "VendorInvoice")
    public VendorInvoice recordInvoicePayment(Long invoiceId, java.math.BigDecimal amount) {
        VendorInvoice invoice = getInvoiceById(invoiceId);
        invoice.setPaidAmount(invoice.getPaidAmount().add(amount));
        if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus("PAID");
        } else {
            invoice.setPaymentStatus("PARTIALLY_PAID");
        }
        return invoiceRepo.save(invoice);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard Metrics
    // ─────────────────────────────────────────────────────────────────────────

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("pendingApprovals", approvalRepo.countByStatus(ProcurementApprovalStatus.PENDING));
        metrics.put("openRfqs", rfqRepo.count()); // simplified
        metrics.put("activeBpos", bpoRepo.count());
        metrics.put("invoiceDiscrepancies", invoiceRepo.countDiscrepancies());
        return metrics;
    }
}
