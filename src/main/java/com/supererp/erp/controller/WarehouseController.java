package com.supererp.erp.controller;

import com.supererp.erp.dto.ApiResponse;
import com.supererp.erp.entity.*;
import com.supererp.erp.enums.StockMovementType;
import com.supererp.erp.rbac.Permissions;
import com.supererp.erp.rbac.annotation.RequiresFeature;
import com.supererp.erp.rbac.annotation.RequiresPermission;
import com.supererp.erp.service.InventoryService;
import com.supererp.erp.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

/**
 * Warehouse Management System controller.
 * Routes: /admin/wms/**
 */
@Controller
@RequestMapping("/admin/wms")
@RequiredArgsConstructor
@RequiresFeature("WMS")
public class WarehouseController {

    private final WarehouseService wmsService;
    private final InventoryService inventoryService;

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping
    @RequiresPermission(Permissions.WMS_WAREHOUSES_VIEW)
    @Transactional(readOnly = true)
    public String dashboard(Model model) {
        model.addAttribute("metrics", wmsService.getDashboardMetrics());
        return "wms/dashboard";
    }

    // ── Warehouses ─────────────────────────────────────────────────────────────

    @GetMapping("/warehouses")
    @RequiresPermission(Permissions.WMS_WAREHOUSES_VIEW)
    @Transactional(readOnly = true)
    public String warehouses(Model model) {
        model.addAttribute("warehouses", wmsService.getAllWarehouses());
        return "wms/warehouses";
    }

    @GetMapping("/warehouses/new")
    @RequiresPermission(Permissions.WMS_WAREHOUSES_MANAGE)
    public String newWarehouseForm(Model model) {
        model.addAttribute("warehouse", new Warehouse());
        return "wms/warehouse-form";
    }

    @GetMapping("/warehouses/{id}/edit")
    @RequiresPermission(Permissions.WMS_WAREHOUSES_MANAGE)
    @Transactional(readOnly = true)
    public String editWarehouseForm(@PathVariable Long id, Model model) {
        model.addAttribute("warehouse", wmsService.getWarehouseById(id));
        return "wms/warehouse-form";
    }

    @PostMapping("/warehouses/save")
    @RequiresPermission(Permissions.WMS_WAREHOUSES_MANAGE)
    public String saveWarehouse(@ModelAttribute Warehouse warehouse, RedirectAttributes ra) {
        try {
            wmsService.saveWarehouse(warehouse);
            ra.addFlashAttribute("success", "Warehouse saved successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/wms/warehouses";
    }

    @PostMapping("/warehouses/{id}/deactivate")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_WAREHOUSES_MANAGE)
    public ResponseEntity<ApiResponse<?>> deactivateWarehouse(@PathVariable Long id) {
        try {
            wmsService.deactivateWarehouse(id);
            return ResponseEntity.ok(ApiResponse.ok("Warehouse deactivated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Locations ──────────────────────────────────────────────────────────────

    @GetMapping("/warehouses/{warehouseId}/locations")
    @RequiresPermission(Permissions.WMS_LOCATIONS_VIEW)
    @Transactional(readOnly = true)
    public String locations(@PathVariable Long warehouseId, Model model) {
        model.addAttribute("warehouse", wmsService.getWarehouseById(warehouseId));
        model.addAttribute("locations", wmsService.getLocationsByWarehouse(warehouseId));
        return "wms/locations";
    }

    @PostMapping("/warehouses/{warehouseId}/locations/save")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_LOCATIONS_MANAGE)
    public ResponseEntity<ApiResponse<?>> saveLocation(@PathVariable Long warehouseId,
                                                        @RequestBody WarehouseLocation location) {
        try {
            Warehouse w = wmsService.getWarehouseById(warehouseId);
            location.setWarehouse(w);
            WarehouseLocation saved = wmsService.saveLocation(location);
            return ResponseEntity.ok(ApiResponse.ok("Location saved.", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/locations/{id}/qr")
    @RequiresPermission(Permissions.WMS_LOCATIONS_VIEW)
    @ResponseBody
    public ResponseEntity<ApiResponse<?>> getLocationQr(@PathVariable Long id) {
        try {
            String qr = wmsService.getLocationQrCode(id);
            return ResponseEntity.ok(ApiResponse.ok("QR generated.", qr));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Stock Balances ─────────────────────────────────────────────────────────

    @GetMapping("/stock")
    @RequiresPermission(Permissions.WMS_STOCK_VIEW)
    @Transactional(readOnly = true)
    public String stockOverview(@RequestParam(required = false) Long warehouseId, Model model) {
        model.addAttribute("warehouses", wmsService.getAllWarehouses());
        model.addAttribute("selectedWarehouseId", warehouseId);
        if (warehouseId != null) {
            model.addAttribute("warehouse", wmsService.getWarehouseById(warehouseId));
            model.addAttribute("stockBalances", wmsService.getStockByWarehouse(warehouseId));
        }
        model.addAttribute("lowStockItems", wmsService.getLowStockItems());
        return "wms/stock";
    }

    // AJAX — update reorder point
    @PostMapping("/stock/reorder")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_STOCK_ADJUST)
    public ResponseEntity<ApiResponse<?>> updateReorderPoint(
            @RequestParam Long itemId, @RequestParam Long locationId,
            @RequestParam BigDecimal reorderPoint, @RequestParam BigDecimal reorderQty) {
        try {
            wmsService.getBalance(itemId, locationId).ifPresent(b -> {
                b.setReorderPoint(reorderPoint);
                b.setReorderQty(reorderQty);
                b.setAlertSent(false);
            });
            return ResponseEntity.ok(ApiResponse.ok("Reorder point updated."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // AJAX — manual stock adjustment
    @PostMapping("/stock/adjust")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_STOCK_ADJUST)
    public ResponseEntity<ApiResponse<?>> adjustStock(
            @RequestParam Long itemId, @RequestParam Long locationId,
            @RequestParam String adjustmentType,
            @RequestParam BigDecimal qty, @RequestParam(required = false) String remarks,
            Principal principal) {
        try {
            StockMovementType type = "IN".equals(adjustmentType)
                ? StockMovementType.ADJUSTMENT_IN
                : StockMovementType.ADJUSTMENT_OUT;
            String txn = "ADJ-" + System.currentTimeMillis();
            wmsService.recordMovement(itemId, locationId, type, qty, null,
                txn, "ADJUSTMENT", null, txn, null, remarks,
                principal != null ? principal.getName() : "system");
            return ResponseEntity.ok(ApiResponse.ok("Stock adjusted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Stock Ledger ───────────────────────────────────────────────────────────

    @GetMapping("/ledger")
    @RequiresPermission(Permissions.WMS_LEDGER_VIEW)
    @Transactional(readOnly = true)
    public String ledger(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "50") int size,
                          @RequestParam(required = false) Long itemId,
                          Model model) {
        model.addAttribute("ledgerPage", itemId != null
            ? wmsService.getLedgerByItem(itemId, page, size)
            : wmsService.getAllLedger(page, size));
        model.addAttribute("items", inventoryService.getAll());
        model.addAttribute("selectedItemId", itemId);
        model.addAttribute("currentPage", page);
        return "wms/ledger";
    }

    // ── Transfers ──────────────────────────────────────────────────────────────

    @GetMapping("/transfers")
    @RequiresPermission(Permissions.WMS_TRANSFERS_VIEW)
    @Transactional(readOnly = true)
    public String transfers(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String q,
                             Model model) {
        model.addAttribute("transferPage", wmsService.getAllTransfers(page, 20, q));
        model.addAttribute("q", q);
        model.addAttribute("currentPage", page);
        return "wms/transfers";
    }

    @GetMapping("/transfers/new")
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    @Transactional(readOnly = true)
    public String newTransferForm(Model model) {
        model.addAttribute("transfer", new StockTransfer());
        model.addAttribute("locations", wmsService.getAllLocations());
        model.addAttribute("items", inventoryService.getAll());
        return "wms/transfer-form";
    }

    @GetMapping("/transfers/{id}")
    @RequiresPermission(Permissions.WMS_TRANSFERS_VIEW)
    @Transactional(readOnly = true)
    public String viewTransfer(@PathVariable Long id, Model model) {
        model.addAttribute("transfer", wmsService.getTransferById(id));
        return "wms/transfer-detail";
    }

    @PostMapping("/transfers/save")
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    public String saveTransfer(@ModelAttribute StockTransfer transfer, Principal principal, RedirectAttributes ra) {
        try {
            String by = principal != null ? principal.getName() : "system";
            wmsService.createTransfer(transfer, by);
            ra.addFlashAttribute("success", "Transfer order created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/wms/transfers";
    }

    @PostMapping("/transfers/{id}/submit")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    public ResponseEntity<ApiResponse<?>> submitTransfer(@PathVariable Long id) {
        try {
            wmsService.submitTransfer(id);
            return ResponseEntity.ok(ApiResponse.ok("Transfer submitted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/transfers/{id}/dispatch")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    public ResponseEntity<ApiResponse<?>> dispatchTransfer(@PathVariable Long id, Principal principal) {
        try {
            wmsService.dispatchTransfer(id, principal != null ? principal.getName() : "system");
            return ResponseEntity.ok(ApiResponse.ok("Transfer dispatched — stock deducted from source."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/transfers/{id}/receive")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    public ResponseEntity<ApiResponse<?>> receiveTransfer(@PathVariable Long id, Principal principal) {
        try {
            wmsService.receiveTransfer(id, principal != null ? principal.getName() : "system");
            return ResponseEntity.ok(ApiResponse.ok("Transfer received — stock added to destination."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/transfers/{id}/cancel")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_TRANSFERS_MANAGE)
    public ResponseEntity<ApiResponse<?>> cancelTransfer(@PathVariable Long id) {
        try {
            wmsService.cancelTransfer(id);
            return ResponseEntity.ok(ApiResponse.ok("Transfer cancelled."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Batch / Lot Tracking ──────────────────────────────────────────────────

    @GetMapping("/batches")
    @RequiresPermission(Permissions.WMS_BATCHES_VIEW)
    @Transactional(readOnly = true)
    public String batches(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) Long itemId,
                           Model model) {
        model.addAttribute("batchPage", itemId != null
            ? null // will be shown as a filter list
            : wmsService.getAllBatchLots(page, 30));
        model.addAttribute("items", inventoryService.getAll());
        model.addAttribute("selectedItemId", itemId);
        if (itemId != null) {
            model.addAttribute("batchList", wmsService.getBatchLotsByItem(itemId));
        }
        model.addAttribute("expiringBatches", wmsService.getExpiringBatchLots(30));
        model.addAttribute("currentPage", page);
        return "wms/batches";
    }

    @PostMapping("/batches/save")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_BATCHES_MANAGE)
    public ResponseEntity<ApiResponse<?>> saveBatchLot(@RequestBody BatchLot batchLot) {
        try {
            BatchLot saved = wmsService.saveBatchLot(batchLot);
            return ResponseEntity.ok(ApiResponse.ok("Batch lot saved.", saved.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/batches/{id}/qr")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_BATCHES_VIEW)
    public ResponseEntity<ApiResponse<?>> getBatchQr(@PathVariable Long id) {
        try {
            String qr = wmsService.getBatchQrCode(id);
            return ResponseEntity.ok(ApiResponse.ok("QR generated.", qr));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Stock Counts ──────────────────────────────────────────────────────────

    @GetMapping("/stockcounts")
    @RequiresPermission(Permissions.WMS_STOCKCOUNT_VIEW)
    @Transactional(readOnly = true)
    public String stockCounts(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("countPage", wmsService.getAllCounts(page, 20));
        model.addAttribute("warehouses", wmsService.getAllWarehouses());
        model.addAttribute("currentPage", page);
        return "wms/stockcounts";
    }

    @GetMapping("/stockcounts/{id}")
    @RequiresPermission(Permissions.WMS_STOCKCOUNT_VIEW)
    @Transactional(readOnly = true)
    public String viewStockCount(@PathVariable Long id, Model model) {
        model.addAttribute("count", wmsService.getCountById(id));
        return "wms/stockcount-detail";
    }

    @PostMapping("/stockcounts/create")
    @RequiresPermission(Permissions.WMS_STOCKCOUNT_MANAGE)
    public String createStockCount(@RequestParam Long warehouseId,
                                    @RequestParam(required = false) Long locationId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate countDate,
                                    Principal principal, RedirectAttributes ra) {
        try {
            Warehouse wh = wmsService.getWarehouseById(warehouseId);
            WarehouseLocation loc = (locationId != null) ? wmsService.getLocationById(locationId) : null;
            String by = principal != null ? principal.getName() : "system";
            StockCount count = wmsService.createStockCount(wh, loc, countDate, by);
            ra.addFlashAttribute("success", "Stock count " + count.getCountNumber() + " created.");
            return "redirect:/admin/wms/stockcounts/" + count.getId();
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/wms/stockcounts";
        }
    }

    @PostMapping("/stockcounts/{id}/record")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_STOCKCOUNT_MANAGE)
    public ResponseEntity<ApiResponse<?>> recordCount(@PathVariable Long id,
            @RequestParam Long itemId, @RequestParam Long locationId,
            @RequestParam BigDecimal countedQty,
            @RequestParam(defaultValue = "false") boolean isScanned) {
        try {
            wmsService.recordCount(id, itemId, locationId, countedQty, isScanned);
            return ResponseEntity.ok(ApiResponse.ok("Count recorded."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/stockcounts/{id}/complete")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_STOCKCOUNT_MANAGE)
    public ResponseEntity<ApiResponse<?>> completeCount(@PathVariable Long id, Principal principal) {
        try {
            String by = principal != null ? principal.getName() : "system";
            wmsService.completeAndPostAdjustments(id, by);
            return ResponseEntity.ok(ApiResponse.ok("Stock count completed and adjustments posted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ── Barcode lookup (scan) ─────────────────────────────────────────────────

    @GetMapping("/scan")
    @RequiresPermission(Permissions.WMS_STOCK_VIEW)
    public String scanPage() {
        return "wms/scan";
    }

    @GetMapping("/scan/lookup")
    @ResponseBody
    @RequiresPermission(Permissions.WMS_STOCK_VIEW)
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> lookupBarcode(@RequestParam String barcode) {
        try {
            var loc = wmsService.findLocationByBarcode(barcode);
            if (loc.isPresent()) {
                WarehouseLocation l = loc.get();
                return ResponseEntity.ok(ApiResponse.ok("Location found.", java.util.Map.of(
                    "type", "LOCATION",
                    "id", l.getId(),
                    "fullAddress", l.getFullAddress(),
                    "warehouse", l.getWarehouse() != null ? l.getWarehouse().getName() : ""
                )));
            }
            return ResponseEntity.ok(ApiResponse.error("Barcode not found: " + barcode));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
