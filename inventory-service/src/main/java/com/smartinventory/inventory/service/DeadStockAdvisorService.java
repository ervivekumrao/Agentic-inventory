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
 * AI Dead Stock & Liquidation Advisor Service:
 * Analyzes slow-moving inventory items (high on-hand quantity with low sales velocity over 14 days)
 * and generates actionable markdown discounts or warehouse transfer recommendations.
 */
@Service
public class DeadStockAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(DeadStockAdvisorService.class);

    private final InventoryRepository inventoryRepository;
    private final SalesHistoryAggregationService salesHistoryAggregationService;

    @Autowired
    public DeadStockAdvisorService(
            InventoryRepository inventoryRepository,
            SalesHistoryAggregationService salesHistoryAggregationService) {
        this.inventoryRepository = inventoryRepository;
        this.salesHistoryAggregationService = salesHistoryAggregationService;
    }

    /**
     * Scans inventory for dead stock and produces liquidation/transfer recommendations.
     */
    @Transactional(readOnly = true)
    public DeadStockReportDto analyzeDeadStock() {
        log.info("AI Dead Stock Advisor auditing slow-moving inventory...");
        List<InventoryItem> items = inventoryRepository.findAll();
        List<DeadStockItemRecommendation> recommendations = new ArrayList<>();

        for (InventoryItem item : items) {
            int onHand = item.getQuantity();
            // Flag items with significant stock (> 25 units)
            if (onHand >= 25) {
                var salesHistory = salesHistoryAggregationService.getSalesHistoryForSku(item.getSku(), 14);
                long fourteenDaySales = salesHistory.totalUnitsSold();

                // If fewer than 3 units sold in 14 days, flag as slow-moving/dead stock
                if (fourteenDaySales <= 3) {
                    int discountPercent = (onHand > 60) ? 30 : 15;
                    String action = (onHand > 75)
                            ? "MARKDOWN_AND_TRANSFER_TO_CENTRAL_HUB"
                            : "APPLY_MARKDOWN_DISCOUNT";

                    recommendations.add(new DeadStockItemRecommendation(
                            item.getSku(),
                            item.getWarehouseLocation(),
                            onHand,
                            fourteenDaySales,
                            discountPercent,
                            action,
                            "Item has " + onHand + " units on hand but only " + fourteenDaySales + " units sold in 14 days."
                    ));
                }
            }
        }

        log.info("AI Dead Stock Advisor flagged {} slow-moving SKUs", recommendations.size());
        return new DeadStockReportDto(
                Instant.now(),
                recommendations.size(),
                recommendations
        );
    }

    public record DeadStockItemRecommendation(
            String sku,
            String warehouseLocation,
            int onHandQuantity,
            long fourteenDaySalesUnits,
            int recommendedDiscountPercentage,
            String recommendedAction,
            String reasoning
    ) {}

    public record DeadStockReportDto(
            Instant generatedAt,
            int flaggedSkuCount,
            List<DeadStockItemRecommendation> recommendations
    ) {}
}
