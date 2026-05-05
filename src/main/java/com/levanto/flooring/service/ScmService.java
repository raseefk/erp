package com.levanto.flooring.service;

import com.levanto.flooring.entity.InventoryItem;
import com.levanto.flooring.entity.PurchaseOrder;
import com.levanto.flooring.entity.PurchaseOrderItem;
import com.levanto.flooring.enums.PurchaseOrderStatus;
import com.levanto.flooring.entity.Expense;
import com.levanto.flooring.enums.ExpenseCategory;
import com.levanto.flooring.repository.ExpenseRepository;
import com.levanto.flooring.repository.InventoryItemRepository;
import com.levanto.flooring.repository.PurchaseOrderItemRepository;
import com.levanto.flooring.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class ScmService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository poiRepo;
    private final InventoryItemRepository itemRepo;
    private final ExpenseRepository expenseRepo;

    public Page<com.levanto.flooring.projection.PurchaseOrderSummary> getAllPOs(int page, int size, String q) {
        Pageable p = PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        return poRepo.searchSummaries(q != null ? q.trim() : null, p);
    }

    public PurchaseOrder getPOById(Long id) {
        return poRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("PO not found: " + id));
    }

    @Transactional
    public PurchaseOrder savePO(PurchaseOrder po) {
        // Calculate total amount from items
        if (po.getItems() != null) {
            po.getItems().forEach(item -> {
                item.setPurchaseOrder(po);
                item.setTotalPrice(item.getUnitPrice().multiply(item.getQuantity()));
            });
            po.setTotalAmount(po.getItems().stream()
                .map(PurchaseOrderItem::getTotalPrice)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        }
        return poRepo.save(po);
    }

    @Transactional
    public void updatePOStatus(Long id, PurchaseOrderStatus status) {
        PurchaseOrder po = getPOById(id);
        PurchaseOrderStatus oldStatus = po.getStatus();
        
        if (oldStatus == status) return;

        po.setStatus(status);
        if (status == PurchaseOrderStatus.RECEIVED) {
            po.setActualDeliveryDate(LocalDate.now());
            // Integrate with Inventory: Increment stock
            po.getItems().forEach(item -> {
                if (item.getInventoryItem() != null) {
                    InventoryItem invItem = item.getInventoryItem();
                    invItem.setStockQuantity(invItem.getStockQuantity() + item.getQuantity().intValue());
                    itemRepo.save(invItem);
                }
            });
        }
        poRepo.save(po);
    }

    @Transactional
    public void markAsPaid(Long id) {
        PurchaseOrder po = getPOById(id);
        if (Boolean.TRUE.equals(po.getPaid())) {
            throw new IllegalStateException("Purchase Order is already marked as paid.");
        }
        if (po.getStatus() == PurchaseOrderStatus.DRAFT || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot mark as paid in status: " + po.getStatus());
        }

        // Create Expense Entry
        Expense expense = Expense.builder()
                .category(ExpenseCategory.PURCHASE_ORDER)
                .description("Purchase Order Payment: " + po.getPoNumber() + " - " + po.getVendor().getName())
                .amount(po.getTotalAmount())
                .expenseDate(LocalDate.now())
                .reference(po.getPoNumber())
                .build();
        
        expenseRepo.save(expense);
        
        po.setPaid(true);
        poRepo.save(po);
    }

    public PurchaseOrderItem getLatestPrice(Long itemId) {
        return poiRepo.findLatestReceivedPrice(itemId).orElse(null);
    }
}
