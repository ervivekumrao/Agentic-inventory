package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.db.dialect.QueryDialect;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;
    private final QueryDialect queryDialect;

    public InventoryController(InventoryService inventoryService, QueryDialect queryDialect) {
        this.inventoryService = inventoryService;
        this.queryDialect = queryDialect;
    }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllInventory(@RequestParam(required = false) Boolean lowStock) {
        log.debug("GET /api/inventory requested with lowStock filter: {}", lowStock);
        if (Boolean.TRUE.equals(lowStock)) {
            List<InventoryItem> lowStockItems = inventoryService.getLowStockItems();
            log.trace("Found {} low-stock items", lowStockItems.size());
            return ResponseEntity.ok(lowStockItems);
        }
        List<InventoryItem> allItems = inventoryService.getAllInventory();
        log.trace("Found {} total inventory items", allItems.size());
        return ResponseEntity.ok(allItems);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<InventoryItem> getInventoryBySku(@PathVariable String sku) {
        log.debug("GET /api/inventory/{} requested", sku);
        return inventoryService.getInventoryBySku(sku)
                .map(item -> {
                    log.trace("Inventory item found for SKU {}: qty={}", sku, item.getQuantity());
                    return ResponseEntity.ok(item);
                })
                .orElseGet(() -> {
                    log.debug("Inventory item not found for SKU: {}", sku);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createInventoryItem(@Valid @RequestBody InventoryItem item) {
        log.info("POST /api/inventory requested for SKU: {}, Quantity: {}", item.getSku(), item.getQuantity());
        InventoryItem created = inventoryService.createInventoryItem(item);
        log.info("Inventory item created successfully for SKU: {}", created.getSku());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<InventoryItem>> createInventoryBulk(@RequestBody List<InventoryItem> items) {
        log.info("POST /api/inventory/bulk requested with {} items", items.size());
        List<InventoryItem> created = inventoryService.createInventoryItemsBulk(items);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{sku}/adjust")
    public ResponseEntity<InventoryItem> adjustStock(
            @PathVariable String sku,
            @RequestBody Map<String, Object> adjustmentRequest) {
        
        int quantityChange = (Integer) adjustmentRequest.getOrDefault("quantityChange", 0);
        String transactionType = (String) adjustmentRequest.getOrDefault("transactionType", "ADJUSTMENT");
        String referenceId = (String) adjustmentRequest.getOrDefault("referenceId", "SYSTEM");

        log.info("POST /api/inventory/{}/adjust requested: qtyChange={}, type={}, ref={}",
                sku, quantityChange, transactionType, referenceId);

        InventoryItem updated = inventoryService.adjustStock(sku, quantityChange, transactionType, referenceId);
        log.info("Stock adjusted successfully for SKU: {}. New Quantity: {}", sku, updated.getQuantity());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{sku}/transactions")
    public ResponseEntity<List<StockTransaction>> getTransactions(@PathVariable String sku) {
        log.debug("GET /api/inventory/{}/transactions requested", sku);
        List<StockTransaction> transactions = inventoryService.getTransactionsForSku(sku);
        log.trace("Found {} transactions for SKU: {}", transactions.size(), sku);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/database-info")
    public ResponseEntity<Map<String, Object>> getDatabaseInfo() {
        log.debug("GET /api/inventory/database-info requested");
        Map<String, Object> metadata = queryDialect.getDatabaseMetadata();
        return ResponseEntity.ok(metadata);
    }
}
