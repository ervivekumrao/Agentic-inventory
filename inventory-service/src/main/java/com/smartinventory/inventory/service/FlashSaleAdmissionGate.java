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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fast-path Redis Admission Control Gate & Token Bucket Queue (WP-17).
 * Atomically filters extreme flash-sale traffic before hitting database lock boundaries.
 */
@Service
public class FlashSaleAdmissionGate {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleAdmissionGate.class);
    private static final String REDIS_GATE_PREFIX = "flash_sale:tokens:";

    private final StringRedisTemplate redisTemplate;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public FlashSaleAdmissionGate(
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            InventoryRepository inventoryRepository) {
        this.redisTemplate = redisTemplate;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Atomically attempts to claim N flash-sale admission tokens from Redis.
     * Returns true if admission tokens were granted, false if rejected due to high load / stock depletion.
     */
    public boolean tryAdmitRequest(String sku, int quantity) {
        if (redisTemplate == null) {
            // Fallback: Admit request to DB layer if Redis is inactive
            return true;
        }

        String key = REDIS_GATE_PREFIX + sku.toUpperCase();
        try {
            Long currentTokens = redisTemplate.opsForValue().decrement(key, quantity);
            if (currentTokens == null) {
                // Key does not exist in Redis yet, initialize from DB
                syncTokensFromDatabase(sku);
                currentTokens = redisTemplate.opsForValue().decrement(key, quantity);
            }

            if (currentTokens != null && currentTokens >= 0) {
                log.debug("FlashSaleAdmissionGate granted admission for SKU: {}, tokens remaining: {}", sku, currentTokens);
                return true;
            } else {
                log.warn("FlashSaleAdmissionGate rejected admission for SKU: {}. Gate depleted!", sku);
                // Restore decremented overage tokens
                redisTemplate.opsForValue().increment(key, quantity);
                return false;
            }
        } catch (Exception e) {
            log.warn("Redis Admission Control Gate error for SKU {}: {}. Falling back to DB.", sku, e.getMessage());
            return true;
        }
    }

    /**
     * Synchronizes available stock tokens from PostgreSQL into Redis cache.
     */
    public void syncTokensFromDatabase(String sku) {
        if (redisTemplate == null) {
            return;
        }

        InventoryItem item = inventoryRepository.findBySku(sku).orElse(null);
        int availableStock = item != null ? Math.max(0, item.getQuantity()) : 0;

        String key = REDIS_GATE_PREFIX + sku.toUpperCase();
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(availableStock), Duration.ofMinutes(10));
            log.info("FlashSaleAdmissionGate synchronized Redis tokens for SKU {}: {} available", sku, availableStock);
        } catch (Exception e) {
            log.warn("Failed to sync Redis admission tokens for SKU {}: {}", sku, e.getMessage());
        }
    }

    /**
     * Scheduled reconciler keeping Redis admission tokens synced with database stock every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    public void reconcileAdmissionGateTokens() {
        if (redisTemplate == null) {
            return;
        }

        try {
            var items = inventoryRepository.findAll();
            for (InventoryItem item : items) {
                syncTokensFromDatabase(item.getSku());
            }
            log.debug("FlashSaleAdmissionGate finished scheduled token reconciliation for {} items", items.size());
        } catch (Exception e) {
            log.warn("Error during scheduled Redis token reconciliation: {}", e.getMessage());
        }
    }
}
