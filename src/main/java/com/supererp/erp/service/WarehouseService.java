package com.supererp.erp.service;

import com.supererp.erp.entity.*;
import com.supererp.erp.enums.*;
import com.supererp.erp.repository.*;
import com.supererp.erp.tenant.TenantContext;
import com.supererp.erp.util.BarcodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Core Warehouse Management Service.
 * Handles warehouses, locations, stock movements (FIFO/LIFO/WAC),
 * transfers, batch/lot tracking, stock counts and barcode generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final WarehouseRepository          warehouseRepo;
    private final WarehouseLocationRepository  locationRepo;
    private final StockBalanceRepository       balanceRepo;
    private final StockLedgerRepository        ledgerRepo;
    private final StockTransferRepository      transferRepo;
    private final BatchLotRepository           batchLotRepo;
    private final StockCountRepository         countRepo;
    private final InventoryItemRepository      itemRepo;
    private final BarcodeUtil                  barcodeUtil;

    // ── Sequence counters (in-memory per JVM — safe for single-node) ─────────

    // ═══════════════════════════════════════════════════════════════════════════
    // WAREHOUSES
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepo.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));
    }

    @Transactional
    public Warehouse saveWarehouse(Warehouse warehouse) {
        // If marked default, clear existing default
        if (Boolean.TRUE.equals(warehouse.getIsDefault())) {
            UUID tid = TenantContext.getTenantId();
            warehouseRepo.findByIsDefaultTrueAndTenantId(tid).ifPresent(existing -> {
                if (!existing.getId().equals(warehouse.getId())) {
                    existing.setIsDefault(false);
                    warehouseRepo.save(existing);
                }
            });
        }
        return warehouseRepo.save(warehouse);
    }

    @Transactional
    public void deactivateWarehouse(Long id) {
        Warehouse w = getWarehouseById(id);
        w.setActive(false);
        warehouseRepo.save(w);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<WarehouseLocation> getLocationsByWarehouse(Long warehouseId) {
        return locationRepo.findByWarehouseWithWarehouse(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<WarehouseLocation> getAllLocations() {
        return locationRepo.findAllActiveWithWarehouse();
    }

    @Transactional(readOnly = true)
    public WarehouseLocation getLocationById(Long id) {
        return locationRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<WarehouseLocation> findLocationByBarcode(String barcode) {
        return locationRepo.findByBarcode(barcode);
    }

    @Transactional
    public WarehouseLocation saveLocation(WarehouseLocation location) {
        // Auto-generate barcode if blank
        if (location.getBarcode() == null || location.getBarcode().isBlank()) {
            String code = "LOC-" + (location.getWarehouse() != null ? location.getWarehouse().getId() : "0")
                    + "-" + System.currentTimeMillis();
            location.setBarcode(code);
        }
        return locationRepo.save(location);
    }

    /**
     * Generate QR code image (Base64) for a location label.
     */
    public String getLocationQrCode(Long locationId) {
        WarehouseLocation loc = getLocationById(locationId);
        String content = "LOC:" + loc.getId() + "|" + loc.getBarcode() + "|" + loc.getFullAddress();
        return barcodeUtil.generateQrCodeBase64(content);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STOCK BALANCE
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<StockBalance> getStockByWarehouse(Long warehouseId) {
        return balanceRepo.findByWarehouseIdWithDetails(warehouseId);
    }

    @Transactional(readOnly = true)
    public List<StockBalance> getStockByItem(Long itemId) {
        return balanceRepo.findByInventoryItemIdWithDetails(itemId);
    }

    @Transactional(readOnly = true)
    public List<StockBalance> getLowStockItems() {
        return balanceRepo.findBelowReorderPointByTenant(TenantContext.getTenantId());
    }

    @Transactional(readOnly = true)
    public Optional<StockBalance> getBalance(Long itemId, Long locationId) {
        return balanceRepo.findByInventoryItemIdAndLocationId(itemId, locationId);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STOCK MOVEMENTS — core engine
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Record a stock movement, update balance and write ledger entry.
     * This is the single point of entry for all stock changes.
     */
    @Transactional
    public StockLedger recordMovement(Long itemId, Long locationId,
                                       StockMovementType type,
                                       BigDecimal qty,
                                       BigDecimal unitCost,
                                       String txnNumber,
                                       String refType, Long refId, String refNumber,
                                       BatchLot batchLot,
                                       String remarks,
                                       String createdBy) {

        InventoryItem item = itemRepo.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        WarehouseLocation location = getLocationById(locationId);

        // Determine signed quantity (IN = positive, OUT = negative)
        boolean isIn = type == StockMovementType.RECEIPT
                || type == StockMovementType.TRANSFER_IN
                || type == StockMovementType.ADJUSTMENT_IN
                || type == StockMovementType.RETURN
                || type == StockMovementType.OPENING;
        BigDecimal signedQty = isIn ? qty.abs() : qty.abs().negate();

        // Get or create balance record
        StockBalance balance = balanceRepo.findByInventoryItemIdAndLocationId(itemId, locationId)
            .orElseGet(() -> StockBalance.builder()
                .inventoryItem(item)
                .location(location)
                .quantityOnHand(BigDecimal.ZERO)
                .quantityReserved(BigDecimal.ZERO)
                .quantityAvailable(BigDecimal.ZERO)
                .avgCost(BigDecimal.ZERO)
                .build());

        BigDecimal prevQty = balance.getQuantityOnHand();
        BigDecimal newQty  = prevQty.add(signedQty);

        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                "Insufficient stock: " + item.getName() + " at " + location.getFullAddress() +
                ". Available: " + prevQty + ", Requested: " + qty);
        }

        // Update weighted average cost (only on IN movements with cost data)
        BigDecimal newAvgCost = balance.getAvgCost();
        if (isIn && unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal prevValue = prevQty.multiply(balance.getAvgCost());
            BigDecimal newValue  = qty.abs().multiply(unitCost);
            newAvgCost = newQty.compareTo(BigDecimal.ZERO) > 0
                ? prevValue.add(newValue).divide(newQty, 4, RoundingMode.HALF_UP)
                : unitCost;
        }

        balance.setQuantityOnHand(newQty);
        balance.setAvgCost(newAvgCost);
        balance.setQuantityAvailable(newQty.subtract(balance.getQuantityReserved()));
        balance.setLastMovementAt(LocalDateTime.now());
        balance.setAlertSent(false); // reset so reorder alert fires again if needed
        balanceRepo.save(balance);

        // Update batch lot if provided
        if (batchLot != null) {
            BigDecimal newBatchAvail = batchLot.getQuantityAvailable().add(signedQty);
            batchLot.setQuantityAvailable(newBatchAvail.max(BigDecimal.ZERO));
            batchLotRepo.save(batchLot);
        }

        // Write ledger (immutable)
        BigDecimal cost = unitCost != null ? unitCost : newAvgCost;
        StockLedger entry = StockLedger.builder()
            .inventoryItem(item)
            .location(location)
            .movementType(type)
            .transactionNumber(txnNumber)
            .quantity(signedQty)
            .unitCost(cost)
            .totalCost(cost.multiply(signedQty.abs()))
            .balanceQty(newQty)
            .movementDate(LocalDate.now())
            .referenceType(refType)
            .referenceId(refId)
            .referenceNumber(refNumber)
            .batchLot(batchLot)
            .remarks(remarks)
            .createdBy(createdBy)
            .build();
        return ledgerRepo.save(entry);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STOCK TRANSFERS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<StockTransfer> getAllTransfers(int page, int size, String q) {
        Pageable pg = PageRequest.of(page, size);
        return (q != null && !q.isBlank())
            ? transferRepo.searchWithDetails(q, pg)
            : transferRepo.findAllWithDetails(pg);
    }

    @Transactional(readOnly = true)
    public StockTransfer getTransferById(Long id) {
        return transferRepo.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));
    }

    @Transactional
    public StockTransfer createTransfer(StockTransfer transfer, String createdBy) {
        transfer.setTransferNumber(generateTransferNumber());
        transfer.setStatus(StockTransferStatus.DRAFT);
        transfer.setCreatedBy(createdBy);
        transfer.getItems().forEach(i -> i.setStockTransfer(transfer));
        return transferRepo.save(transfer);
    }

    @Transactional
    public StockTransfer submitTransfer(Long id) {
        StockTransfer transfer = getTransferById(id);
        assertStatus(transfer.getStatus(), StockTransferStatus.DRAFT, "submit");
        transfer.setStatus(StockTransferStatus.SUBMITTED);
        return transferRepo.save(transfer);
    }

    @Transactional
    public StockTransfer dispatchTransfer(Long id, String dispatchedBy) {
        StockTransfer transfer = getTransferById(id);
        assertStatus(transfer.getStatus(), StockTransferStatus.SUBMITTED, "dispatch");

        // Deduct stock from source location
        for (StockTransferItem item : transfer.getItems()) {
            String txn = transfer.getTransferNumber();
            recordMovement(
                item.getInventoryItem().getId(),
                transfer.getFromLocation().getId(),
                StockMovementType.TRANSFER_OUT,
                item.getRequestedQty(),
                null, txn, "TRANSFER", transfer.getId(), txn,
                item.getBatchLot(), "Transfer dispatch", dispatchedBy);
            item.setTransferredQty(item.getRequestedQty());
        }

        transfer.setStatus(StockTransferStatus.IN_TRANSIT);
        transfer.setTransferDate(LocalDate.now());
        return transferRepo.save(transfer);
    }

    @Transactional
    public StockTransfer receiveTransfer(Long id, String receivedBy) {
        StockTransfer transfer = getTransferById(id);
        assertStatus(transfer.getStatus(), StockTransferStatus.IN_TRANSIT, "receive");

        // Add stock to destination location
        for (StockTransferItem item : transfer.getItems()) {
            String txn = transfer.getTransferNumber();
            recordMovement(
                item.getInventoryItem().getId(),
                transfer.getToLocation().getId(),
                StockMovementType.TRANSFER_IN,
                item.getTransferredQty(),
                null, txn, "TRANSFER", transfer.getId(), txn,
                item.getBatchLot(), "Transfer receipt", receivedBy);
            item.setReceivedQty(item.getTransferredQty());
        }

        transfer.setStatus(StockTransferStatus.RECEIVED);
        transfer.setReceivedDate(LocalDate.now());
        transfer.setReceivedBy(receivedBy);
        return transferRepo.save(transfer);
    }

    @Transactional
    public StockTransfer cancelTransfer(Long id) {
        StockTransfer transfer = getTransferById(id);
        if (transfer.getStatus() == StockTransferStatus.RECEIVED) {
            throw new IllegalStateException("Cannot cancel a received transfer.");
        }
        // If already in transit, reverse the OUT movement
        if (transfer.getStatus() == StockTransferStatus.IN_TRANSIT) {
            for (StockTransferItem item : transfer.getItems()) {
                recordMovement(
                    item.getInventoryItem().getId(),
                    transfer.getFromLocation().getId(),
                    StockMovementType.ADJUSTMENT_IN,
                    item.getTransferredQty(),
                    null, transfer.getTransferNumber() + "-REV",
                    "TRANSFER_CANCEL", transfer.getId(), transfer.getTransferNumber(),
                    null, "Transfer cancelled — stock reversed", "system");
            }
        }
        transfer.setStatus(StockTransferStatus.CANCELLED);
        return transferRepo.save(transfer);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BATCH / LOT TRACKING
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<BatchLot> getAllBatchLots(int page, int size) {
        return batchLotRepo.findAllWithItem(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<BatchLot> getBatchLotsByItem(Long itemId) {
        return batchLotRepo.findByItemIdActive(itemId);
    }

    @Transactional(readOnly = true)
    public List<BatchLot> getExpiringBatchLots(int days) {
        return batchLotRepo.findExpiringBetween(LocalDate.now(), LocalDate.now().plusDays(days));
    }

    @Transactional
    public BatchLot saveBatchLot(BatchLot batchLot) {
        if (batchLot.getQrCodeData() == null || batchLot.getQrCodeData().isBlank()) {
            batchLot.setQrCodeData(buildBatchQrContent(batchLot));
        }
        return batchLotRepo.save(batchLot);
    }

    public String getBatchQrCode(Long batchLotId) {
        BatchLot bl = batchLotRepo.findById(batchLotId)
            .orElseThrow(() -> new IllegalArgumentException("Batch lot not found: " + batchLotId));
        String content = bl.getQrCodeData() != null ? bl.getQrCodeData() : buildBatchQrContent(bl);
        return barcodeUtil.generateQrCodeBase64(content);
    }

    private String buildBatchQrContent(BatchLot bl) {
        return "BATCH:" + bl.getBatchNumber()
            + "|ITEM:" + (bl.getInventoryItem() != null ? bl.getInventoryItem().getId() : "")
            + (bl.getExpiryDate() != null ? "|EXP:" + bl.getExpiryDate() : "");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STOCK COUNTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<StockCount> getAllCounts(int page, int size) {
        return countRepo.findAllWithWarehouse(PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public StockCount getCountById(Long id) {
        return countRepo.findByIdWithItems(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock count not found: " + id));
    }

    /**
     * Create a new stock count and pre-populate items from current balances.
     */
    @Transactional
    public StockCount createStockCount(Warehouse warehouse, WarehouseLocation location,
                                        LocalDate countDate, String conductedBy) {
        String countNumber = generateCountNumber();

        // Pre-populate items from stock balances
        List<StockBalance> balances = (location != null)
            ? balanceRepo.findByLocationIdWithDetails(location.getId())
            : balanceRepo.findByWarehouseIdWithDetails(warehouse.getId());

        List<StockCountItem> items = new java.util.ArrayList<>(
            balances.stream()
                .filter(b -> b.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0)
                .map(b -> (StockCountItem) StockCountItem.builder()
                    .inventoryItem(b.getInventoryItem())
                    .location(b.getLocation())
                    .systemQty(b.getQuantityOnHand())
                    .countedQty(BigDecimal.ZERO)
                    .varianceQty(b.getQuantityOnHand().negate())
                    .unit(b.getInventoryItem().getUnit())
                    .build())
                .collect(java.util.stream.Collectors.toList()));

        StockCount count = StockCount.builder()
            .countNumber(countNumber)
            .warehouse(warehouse)
            .location(location)
            .status(StockCountStatus.DRAFT)
            .countDate(countDate)
            .conductedBy(conductedBy)
            .items(new java.util.ArrayList<>(items))
            .build();

        // Set back-reference
        count.getItems().forEach(i -> i.setStockCount(count));
        return countRepo.save(count);
    }

    /**
     * Record a counted quantity for one line item (called per barcode scan or manual entry).
     */
    @Transactional
    public StockCountItem recordCount(Long countId, Long itemId, Long locationId,
                                       BigDecimal countedQty, boolean isScanned) {
        StockCount count = getCountById(countId);
        if (count.getStatus() == StockCountStatus.COMPLETED || count.getStatus() == StockCountStatus.CANCELLED) {
            throw new IllegalStateException("Count is already " + count.getStatus());
        }
        if (count.getStatus() == StockCountStatus.DRAFT) {
            count.setStatus(StockCountStatus.IN_PROGRESS);
        }

        StockCountItem item = count.getItems().stream()
            .filter(i -> i.getInventoryItem().getId().equals(itemId) && i.getLocation().getId().equals(locationId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Item not in count list"));

        item.setCountedQty(countedQty);
        item.setVarianceQty(countedQty.subtract(item.getSystemQty()));
        item.setIsScanned(isScanned);
        countRepo.save(count);
        return item;
    }

    /**
     * Complete count and post adjustment entries for variances.
     */
    @Transactional
    public StockCount completeAndPostAdjustments(Long countId, String approvedBy) {
        StockCount count = getCountById(countId);
        if (count.getAdjustmentPosted()) {
            throw new IllegalStateException("Adjustments already posted for this count.");
        }

        for (StockCountItem item : count.getItems()) {
            BigDecimal variance = item.getVarianceQty();
            if (variance.compareTo(BigDecimal.ZERO) == 0) continue;

            StockMovementType type = variance.compareTo(BigDecimal.ZERO) > 0
                ? StockMovementType.ADJUSTMENT_IN
                : StockMovementType.ADJUSTMENT_OUT;

            recordMovement(
                item.getInventoryItem().getId(),
                item.getLocation().getId(),
                type,
                variance.abs(),
                null,
                count.getCountNumber(),
                "STOCKCOUNT", count.getId(), count.getCountNumber(),
                null,
                "Stocktake adjustment",
                approvedBy);
        }

        count.setStatus(StockCountStatus.COMPLETED);
        count.setCompletedAt(LocalDateTime.now());
        count.setApprovedBy(approvedBy);
        count.setApprovedAt(LocalDateTime.now());
        count.setAdjustmentPosted(true);
        return countRepo.save(count);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STOCK LEDGER
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Page<StockLedger> getLedgerByItem(Long itemId, int page, int size) {
        return ledgerRepo.findByItemIdWithDetails(itemId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Page<StockLedger> getAllLedger(int page, int size) {
        return ledgerRepo.findAllWithDetails(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REORDER ALERTS — scheduled daily at 7 AM
    // ═══════════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional(readOnly = true)
    public void checkReorderAlerts() {
        List<StockBalance> low = balanceRepo.findBelowReorderPointByTenant(TenantContext.getTenantId());
        for (StockBalance sb : low) {
            if (!Boolean.TRUE.equals(sb.getAlertSent())) {
                log.warn("REORDER ALERT: Item '{}' at '{}' — Available: {}, Reorder Point: {}",
                    sb.getInventoryItem().getName(),
                    sb.getLocation().getFullAddress(),
                    sb.getQuantityAvailable(),
                    sb.getReorderPoint());
                // TODO: integrate with notification service (email / in-app)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DASHBOARD METRICS
    // ═══════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalWarehouses", warehouseRepo.findByActiveTrueOrderByNameAsc().size());
        m.put("totalLocations", locationRepo.findByActiveTrueOrderByFullAddressAsc().size());
        m.put("lowStockItems", balanceRepo.findBelowReorderPointByTenant(TenantContext.getTenantId()).size());
        m.put("expiringBatches", batchLotRepo.findExpiringBetween(LocalDate.now(), LocalDate.now().plusDays(30)).size());
        m.put("warehouses", warehouseRepo.findByActiveTrueOrderByNameAsc());
        m.put("lowStockList", balanceRepo.findBelowReorderPointByTenant(TenantContext.getTenantId()));
        m.put("expiringBatchList", batchLotRepo.findExpiringBetween(LocalDate.now(), LocalDate.now().plusDays(30)));
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private String generateTransferNumber() {
        String prefix = "TRF-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        long count = transferRepo.count() + 1;
        return prefix + String.format("%04d", count);
    }

    private String generateCountNumber() {
        String prefix = "CNT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM")) + "-";
        long count = countRepo.count() + 1;
        return prefix + String.format("%04d", count);
    }

    private void assertStatus(StockTransferStatus current, StockTransferStatus expected, String action) {
        if (current != expected) {
            throw new IllegalStateException(
                "Cannot " + action + " transfer in status: " + current + ". Expected: " + expected);
        }
    }
}
