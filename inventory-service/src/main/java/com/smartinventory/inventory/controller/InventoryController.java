/*
 * Copyright (c) 2026 Vivek Umrao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.db.dialect.QueryDialect;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/inventory", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryItem> createInventoryItem(@Valid @RequestBody InventoryItem item) {
        log.info("POST /api/inventory requested for SKU: {}, Quantity: {}", item.getSku(), item.getQuantity());
        InventoryItem created = inventoryService.createInventoryItem(item);
        log.info("Inventory item created successfully for SKU: {}", created.getSku());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/bulk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<InventoryItem>> createInventoryBulk(@RequestBody List<InventoryItem> items) {
        log.info("POST /api/inventory/bulk requested with {} items", items.size());
        List<InventoryItem> created = inventoryService.createInventoryItemsBulk(items);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{sku}/adjust")
    public ResponseEntity<InventoryItem> adjustStock(
            @PathVariable String sku,
            @RequestBody Map<String, Object> adjustmentRequest) {
        
        int quantityChange = 0;
        Object qtyObj = adjustmentRequest.get("quantityChange");
        if (qtyObj instanceof Number num) {
            quantityChange = num.intValue();
        } else if (qtyObj instanceof String str) {
            try {
                quantityChange = Integer.parseInt(str);
            } catch (NumberFormatException ignored) {}
        }
        String transactionType = adjustmentRequest.get("transactionType") != null ? String.valueOf(adjustmentRequest.get("transactionType")) : "ADJUSTMENT";
        String referenceId = adjustmentRequest.get("referenceId") != null ? String.valueOf(adjustmentRequest.get("referenceId")) : "SYSTEM";

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
