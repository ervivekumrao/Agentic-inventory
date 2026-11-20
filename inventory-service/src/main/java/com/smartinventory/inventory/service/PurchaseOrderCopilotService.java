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

package com.smartinventory.inventory.service;

import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Purchase Order Copilot Service:
 * Evaluates current stock positions against safety stock thresholds and demand forecasts
 * to generate structured, human-approvable draft Purchase Orders.
 */
@Service
public class PurchaseOrderCopilotService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderCopilotService.class);

    private final InventoryRepository inventoryRepository;
    private final SalesHistoryAggregationService salesHistoryAggregationService;

    @Autowired
    public PurchaseOrderCopilotService(
            InventoryRepository inventoryRepository,
            SalesHistoryAggregationService salesHistoryAggregationService) {
        this.inventoryRepository = inventoryRepository;
        this.salesHistoryAggregationService = salesHistoryAggregationService;
    }

    /**
     * Generates a human-approvable draft Purchase Order for all SKUs below reorder threshold or low safety stock.
     */
    @Transactional(readOnly = true)
    public PurchaseOrderDraftDto generateDraftPurchaseOrder() {
        log.info("AI Purchase Order Copilot evaluating stock levels for reorder draft generation...");
        List<InventoryItem> items = inventoryRepository.findAll();
        List<PurchaseOrderItemDraft> orderItems = new ArrayList<>();

        long totalReorderUnits = 0;

        for (InventoryItem item : items) {
            int currentQty = item.getQuantity();
            int reorderPoint = item.getReorderThreshold();

            if (currentQty <= reorderPoint) {
                // Calculate 30-day sales velocity
                var salesHistory = salesHistoryAggregationService.getSalesHistoryForSku(item.getSku(), 30);
                long thirtyDaySales = salesHistory.totalUnitsSold();

                // Target stock level = max(reorderPoint * 3, 30-day sales velocity + safety buffer)
                int targetStock = (int) Math.max(reorderPoint * 3L, thirtyDaySales + 20);
                int recommendedOrderQty = Math.max(10, targetStock - currentQty);

                orderItems.add(new PurchaseOrderItemDraft(
                        item.getSku(),
                        item.getWarehouseLocation(),
                        currentQty,
                        reorderPoint,
                        recommendedOrderQty,
                        7, // Default supplier lead time: 7 days
                        "AUTOMATIC_STOCKOUT_PREVENTION"
                ));
                totalReorderUnits += recommendedOrderQty;
            }
        }

        String poNumber = "PO-DRAFT-" + Instant.now().toEpochMilli();
        log.info("AI Purchase Order Copilot generated draft {} with {} line items (total units: {})",
                poNumber, orderItems.size(), totalReorderUnits);

        return new PurchaseOrderDraftDto(
                poNumber,
                Instant.now(),
                "PENDING_HUMAN_APPROVAL",
                orderItems.size(),
                totalReorderUnits,
                orderItems
        );
    }

    public record PurchaseOrderItemDraft(
            String sku,
            String warehouseLocation,
            int currentQuantity,
            int reorderThreshold,
            int recommendedOrderQuantity,
            int estimatedLeadTimeDays,
            String reorderReason
    ) {}

    public record PurchaseOrderDraftDto(
            String purchaseOrderNumber,
            Instant createdAt,
            String status,
            int totalLineItems,
            long totalUnitsToOrder,
            List<PurchaseOrderItemDraft> lineItems
    ) {}
}
