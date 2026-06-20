package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProcurementService — GRN, 3-way match, approval matrix.
 */
@ExtendWith(MockitoExtension.class)
class ProcurementServiceTest {

    @Mock GoodsReceiptNoteRepository grnRepo;
    @Mock VendorInvoiceRepository invoiceRepo;
    @Mock VendorRatingRepository ratingRepo;
    @Mock RequestForQuotationRepository rfqRepo;
    @Mock BlanketPurchaseOrderRepository bpoRepo;
    @Mock ProcurementApprovalRepository approvalRepo;
    @Mock LandedCostAllocationRepository landedCostRepo;
    @Mock PurchaseOrderRepository poRepo;
    @Mock InventoryItemRepository inventoryRepo;
    @Mock VendorRepository vendorRepo;

    @InjectMocks ProcurementService procurementService;

    // ─── GRN Tests ──────────────────────────────────────────────────────────

    @Test
    void submitGrn_shouldGenerateGrnNumberAndSetStatusToSubmitted() {
        // given
        PurchaseOrder po = buildPo();
        GoodsReceiptNote grn = buildGrn(po);
        grn.setGrnNumber(null); // no number set

        when(grnRepo.save(any(GoodsReceiptNote.class))).thenAnswer(inv -> inv.getArgument(0));

        // when
        GoodsReceiptNote result = procurementService.submitGrn(grn);

        // then
        assertThat(result.getGrnNumber()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo(GrnStatus.SUBMITTED);
        assertThat(result.getReceivedDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void submitGrn_shouldCalculateTotalFromItems() {
        PurchaseOrder po = buildPo();
        GoodsReceiptNote grn = buildGrn(po);

        GrnItem item = new GrnItem();
        item.setDescription("Test Item");
        item.setUnit("KG");
        item.setAcceptedQuantity(new BigDecimal("10"));
        item.setReceivedQuantity(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100"));
        item.setRejectedQuantity(BigDecimal.ZERO);
        grn.getItems().add(item);

        when(grnRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoodsReceiptNote result = procurementService.submitGrn(grn);

        assertThat(result.getTotalReceivedValue()).isEqualByComparingTo("1000.00");
    }

    @Test
    void acceptGrn_shouldUpdateInventoryAndPoStatus() {
        PurchaseOrder po = buildPo();
        po.setStatus(PurchaseOrderStatus.ORDERED);

        InventoryItem invItem = new InventoryItem();
        invItem.setId(1L);
        invItem.setStockQuantity(10);

        GrnItem item = new GrnItem();
        item.setInventoryItem(invItem);
        item.setAcceptedQuantity(new BigDecimal("5"));

        GoodsReceiptNote grn = buildGrn(po);
        grn.setStatus(GrnStatus.SUBMITTED);
        grn.getItems().add(item);

        when(grnRepo.findById(1L)).thenReturn(Optional.of(grn));
        when(grnRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GoodsReceiptNote result = procurementService.acceptGrn(1L, "All good");

        assertThat(result.getStatus()).isEqualTo(GrnStatus.ACCEPTED);
        assertThat(invItem.getStockQuantity()).isEqualTo(15); // 10 + 5
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
    }

    @Test
    void acceptGrn_shouldThrowIfNotSubmitted() {
        GoodsReceiptNote grn = buildGrn(buildPo());
        grn.setStatus(GrnStatus.DRAFT);

        when(grnRepo.findById(1L)).thenReturn(Optional.of(grn));

        assertThatThrownBy(() -> procurementService.acceptGrn(1L, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUBMITTED");
    }

    // ─── 3-Way Match Tests ───────────────────────────────────────────────────

    @Test
    void submitVendorInvoice_fullMatchWhenAmountsWithinTolerance() {
        PurchaseOrder po = buildPo();
        po.setTotalAmount(new BigDecimal("10000"));

        GoodsReceiptNote grn = buildGrn(po);
        grn.setTotalReceivedValue(new BigDecimal("10050")); // within 1% tolerance

        VendorInvoice invoice = new VendorInvoice();
        invoice.setPurchaseOrder(po);
        invoice.setGrn(grn);
        invoice.setVendor(po.getVendor());
        invoice.setInvoiceAmount(new BigDecimal("10050"));
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setTotalAmount(new BigDecimal("10050"));

        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorInvoice result = procurementService.submitVendorInvoice(invoice);

        assertThat(result.getMatchStatus()).isEqualTo("FULL_MATCH");
        assertThat(result.getPoAmountMatch()).isTrue();
        assertThat(result.getGrnAmountMatch()).isTrue();
    }

    @Test
    void submitVendorInvoice_discrepancyWhenAmountDiffers() {
        PurchaseOrder po = buildPo();
        po.setTotalAmount(new BigDecimal("10000"));

        GoodsReceiptNote grn = buildGrn(po);
        grn.setTotalReceivedValue(new BigDecimal("10000"));

        VendorInvoice invoice = new VendorInvoice();
        invoice.setPurchaseOrder(po);
        invoice.setGrn(grn);
        invoice.setVendor(po.getVendor());
        invoice.setTotalAmount(new BigDecimal("12000")); // 20% over — discrepancy
        invoice.setInvoiceAmount(new BigDecimal("12000"));
        invoice.setTaxAmount(BigDecimal.ZERO);

        when(invoiceRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorInvoice result = procurementService.submitVendorInvoice(invoice);

        assertThat(result.getMatchStatus()).isEqualTo("DISCREPANCY");
        assertThat(result.getPoAmountMatch()).isFalse();
    }

    // ─── Vendor Rating Tests ─────────────────────────────────────────────────

    @Test
    void saveVendorRating_calculatesOverallScoreAsAverageOfFour() {
        VendorRating rating = new VendorRating();
        rating.setQualityScore(new BigDecimal("4.0"));
        rating.setDeliveryScore(new BigDecimal("3.0"));
        rating.setPriceScore(new BigDecimal("5.0"));
        rating.setServiceScore(new BigDecimal("4.0"));
        rating.setVendor(new Vendor());

        when(ratingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorRating result = procurementService.saveVendorRating(rating);

        // Average of 4.0, 3.0, 5.0, 4.0 = 4.0
        assertThat(result.getOverallScore()).isEqualByComparingTo("4.00");
    }

    // ─── Approval Matrix Tests ───────────────────────────────────────────────

    @Test
    void triggerApprovalWorkflow_singleLevelForSmallAmount() {
        PurchaseOrder po = buildPo();
        po.setTotalAmount(new BigDecimal("25000")); // ≤ 50,000 → L1 only

        when(approvalRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ProcurementApproval> approvals = procurementService.triggerApprovalWorkflow(po);

        assertThat(approvals).hasSize(1);
    }

    @Test
    void triggerApprovalWorkflow_twoLevelsForMidAmount() {
        PurchaseOrder po = buildPo();
        po.setTotalAmount(new BigDecimal("200000")); // > 50k, ≤ 5L → L1 + L2

        when(approvalRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ProcurementApproval> approvals = procurementService.triggerApprovalWorkflow(po);

        assertThat(approvals).hasSize(2);
    }

    @Test
    void triggerApprovalWorkflow_threeLevelsForLargeAmount() {
        PurchaseOrder po = buildPo();
        po.setTotalAmount(new BigDecimal("1000000")); // > 5L → L1 + L2 + L3

        when(approvalRepo.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<ProcurementApproval> approvals = procurementService.triggerApprovalWorkflow(po);

        assertThat(approvals).hasSize(3);
    }

    @Test
    void approveRequest_whenAllApproved_updatesPOToOrdered() {
        PurchaseOrder po = buildPo();
        po.setStatus(PurchaseOrderStatus.DRAFT);

        ProcurementApproval approval = ProcurementApproval.builder()
                .id(1L).purchaseOrder(po).approvalLevel(1)
                .approverRole("MANAGER").status(ProcurementApprovalStatus.PENDING).build();

        when(approvalRepo.findById(1L)).thenReturn(Optional.of(approval));
        when(approvalRepo.findByPurchaseOrderIdOrderByApprovalLevel(any()))
                .thenReturn(List.of(approval));
        when(approvalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcurementApproval result = procurementService.approveRequest(1L, "LGTM", "Manager1");

        assertThat(result.getStatus()).isEqualTo(ProcurementApprovalStatus.APPROVED);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.ORDERED);
    }

    @Test
    void rejectRequest_cancelsPO() {
        PurchaseOrder po = buildPo();
        po.setStatus(PurchaseOrderStatus.DRAFT);

        ProcurementApproval approval = ProcurementApproval.builder()
                .id(1L).purchaseOrder(po).approvalLevel(1)
                .approverRole("MANAGER").status(ProcurementApprovalStatus.PENDING).build();

        when(approvalRepo.findById(1L)).thenReturn(Optional.of(approval));
        when(approvalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProcurementApproval result = procurementService.rejectRequest(1L, "Budget exceeded", "Manager1");

        assertThat(result.getStatus()).isEqualTo(ProcurementApprovalStatus.REJECTED);
        assertThat(po.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
    }

    // ─── Landed Cost Tests ───────────────────────────────────────────────────

    @Test
    void getTotalLandedCostForPo_returnsZeroWhenNone() {
        when(landedCostRepo.sumByPurchaseOrderId(any())).thenReturn(null);

        BigDecimal total = procurementService.getTotalLandedCostForPo(1L);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private PurchaseOrder buildPo() {
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setName("Test Vendor");

        PurchaseOrder po = new PurchaseOrder();
        po.setId(1L);
        po.setPoNumber("PO-TEST-001");
        po.setVendor(vendor);
        po.setTotalAmount(new BigDecimal("50000"));
        po.setStatus(PurchaseOrderStatus.ORDERED);
        return po;
    }

    private GoodsReceiptNote buildGrn(PurchaseOrder po) {
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setId(1L);
        grn.setPurchaseOrder(po);
        grn.setReceivedDate(LocalDate.now());
        grn.setTotalReceivedValue(BigDecimal.ZERO);
        return grn;
    }
}
