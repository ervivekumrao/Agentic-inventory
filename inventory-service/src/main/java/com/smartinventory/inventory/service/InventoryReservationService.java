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

import com.smartinventory.inventory.kafka.KafkaProducerService;
import com.smartinventory.inventory.kafka.StockUpdateEvent;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.InventoryReservationUnitRepository;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * High-Scale Inventory Reservation Service implementing Shopify's bounded unit-pool
 * non-blocking locking architecture (FOR UPDATE SKIP LOCKED).
 *
 * Eliminates database hot-row serialization and lock wait timeouts during peak flash sales.
 */
@Service
public class InventoryReservationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationService.class);
    public static final int DEFAULT_MAX_POOL_SIZE = 500;
    public static final int DEFAULT_TTL_SECONDS = 600; // 10 minutes

    private final InventoryReservationUnitRepository unitRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    public InventoryReservationService(
            InventoryReservationUnitRepository unitRepository,
            InventoryRepository inventoryRepository,
            StockTransactionRepository transactionRepository,
            KafkaProducerService kafkaProducerService) {
        this.unitRepository = unitRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Atomically reserves inventory units using FOR UPDATE SKIP LOCKED.
     * Prevents lock contention and allows thousands of concurrent checkout requests to proceed in parallel.
     */
    @Transactional
    public ReservationResult reserveStock(String sku, int quantity, String clientReservationId, Integer ttlSeconds) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Reservation quantity must be strictly positive");
        }

        String reservationId = (clientReservationId != null && !clientReservationId.isBlank())
                ? clientReservationId.trim()
                : UUID.randomUUID().toString();

        // 1. Idempotency Check: if this reservation ID already reserved units, return existing reservation
        List<InventoryReservationUnit> existing = unitRepository.findByReservationId(reservationId);
        if (!existing.isEmpty()) {
            log.info("Idempotent reservation replay detected for reservationId: {}, units: {}", reservationId, existing.size());
            return new ReservationResult(
                    reservationId,
                    sku,
                    existing.size(),
                    existing.get(0).getStatus(),
                    existing.get(0).getExpiresAt(),
                    "Idempotent reservation retrieved"
            );
        }

        // 2. Fetch available units non-blocking with SKIP LOCKED
        List<InventoryReservationUnit> lockedUnits = unitRepository.fetchAvailableUnitsForUpdate(sku, quantity);

        // 3. Inline Pool Replenishment: if pool has fewer available units than requested, check master ledger
        if (lockedUnits.size() < quantity) {
            replenishPoolFromMasterLedger(sku, quantity);
            lockedUnits = unitRepository.fetchAvailableUnitsForUpdate(sku, quantity);
        }

        if (lockedUnits.size() < quantity) {
            log.warn("Insufficient stock available in reservation pool for SKU: {}. Requested: {}, Available: {}",
                    sku, quantity, lockedUnits.size());
            throw new IllegalArgumentException("Insufficient inventory available to reserve " + quantity + " units for SKU: " + sku);
        }

        int ttl = (ttlSeconds != null && ttlSeconds > 0) ? ttlSeconds : DEFAULT_TTL_SECONDS;
        Instant expiresAt = Instant.now().plusSeconds(ttl);

        for (InventoryReservationUnit unit : lockedUnits) {
            unit.reserve(reservationId, expiresAt);
        }
        unitRepository.saveAll(lockedUnits);

        log.info("Successfully reserved {} units for SKU: {} with reservationId: {} expiring at: {}",
                lockedUnits.size(), sku, reservationId, expiresAt);

        return new ReservationResult(
                reservationId,
                sku,
                lockedUnits.size(),
                InventoryReservationUnit.STATUS_RESERVED,
                expiresAt,
                "Stock successfully reserved with non-blocking SKIP LOCKED"
        );
    }

    /**
     * Claims a previously reserved batch upon successful payment completion.
     * Permanently decrements the master ledger and records the stock transaction.
     */
    @Transactional
    public boolean claimReservation(String reservationId) {
        List<InventoryReservationUnit> units = unitRepository.findByReservationId(reservationId);
        if (units.isEmpty()) {
            log.warn("Cannot claim reservation: no units found for ID: {}", reservationId);
            return false;
        }

        String sku = units.get(0).getSku();
        int count = 0;
        for (InventoryReservationUnit unit : units) {
            if (InventoryReservationUnit.STATUS_RESERVED.equals(unit.getStatus())) {
                unit.claim();
                count++;
            }
        }

        if (count == 0) {
            log.warn("No active RESERVED units to claim for reservationId: {}", reservationId);
            return false;
        }

        unitRepository.saveAll(units);

        // Permanent deduction from master ledger
        InventoryItem item = inventoryRepository.findBySku(sku).orElse(null);
        if (item != null) {
            int newQty = Math.max(0, item.getQuantity() - count);
            item.setQuantity(newQty);
            inventoryRepository.save(item);
            transactionRepository.save(new StockTransaction(sku, -count, "RESERVATION_CLAIM", reservationId));
            kafkaProducerService.sendStockUpdateEvent(new StockUpdateEvent(sku, newQty, "RESERVATION_CLAIMED"));
        }

        log.info("Successfully claimed {} reserved units for SKU: {} (reservationId: {})", count, sku, reservationId);
        return true;
    }

    /**
     * Releases a reservation (e.g. buyer abandoned cart, payment declined, or cancelled).
     * Units immediately return to AVAILABLE status in the pool without master ledger writes.
     */
    @Transactional
    public boolean releaseReservation(String reservationId) {
        List<InventoryReservationUnit> units = unitRepository.findByReservationId(reservationId);
        if (units.isEmpty()) {
            return false;
        }

        int count = 0;
        for (InventoryReservationUnit unit : units) {
            if (InventoryReservationUnit.STATUS_RESERVED.equals(unit.getStatus())) {
                unit.release();
                count++;
            }
        }

        if (count > 0) {
            unitRepository.saveAll(units);
            log.info("Released {} reserved units back to available pool for reservationId: {}", count, reservationId);
            return true;
        }
        return false;
    }

    /**
     * Automated Reaper: Runs periodically to reclaim expired reservations.
     * Any reservation exceeding its TTL is freed back to AVAILABLE for other buyers.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void cleanupExpiredReservations() {
        List<InventoryReservationUnit> expired = unitRepository.findByStatusAndExpiresAtBefore(
                InventoryReservationUnit.STATUS_RESERVED,
                Instant.now()
        );

        if (!expired.isEmpty()) {
            for (InventoryReservationUnit unit : expired) {
                unit.release();
            }
            unitRepository.saveAll(expired);
            log.info("Auto-reaper reclaimed {} expired reservation units back to pool", expired.size());
        }
    }

    /**
     * Replenishes the bounded unit pool from the master InventoryItem ledger.
     */
    @Transactional
    public synchronized void replenishPoolFromMasterLedger(String sku, int minimumNeeded) {
        InventoryItem item = inventoryRepository.findBySku(sku).orElse(null);
        if (item == null) {
            return;
        }

        long currentAvailable = unitRepository.countBySkuAndStatus(sku, InventoryReservationUnit.STATUS_AVAILABLE);
        long currentReserved = unitRepository.countBySkuAndStatus(sku, InventoryReservationUnit.STATUS_RESERVED);
        long activeUnits = currentAvailable + currentReserved;

        long unpooledStock = item.getQuantity() - activeUnits;
        if (unpooledStock <= 0) {
            return;
        }

        long unitsToCreate = Math.min(unpooledStock, (long) Math.max(minimumNeeded, DEFAULT_MAX_POOL_SIZE - currentAvailable));
        if (unitsToCreate > 0) {
            List<InventoryReservationUnit> newUnits = new ArrayList<>();
            for (int i = 0; i < unitsToCreate; i++) {
                newUnits.add(new InventoryReservationUnit(sku));
            }
            unitRepository.saveAll(newUnits);
            log.info("Replenished reservation pool for SKU: {} with {} new units (Pool available now: {})",
                    sku, unitsToCreate, currentAvailable + unitsToCreate);
        }
    }

    /**
     * Returns pool metrics for observability and UI visualization.
     */
    @Transactional(readOnly = true)
    public PoolStatusDto getPoolStatus(String sku) {
        long available = unitRepository.countBySkuAndStatus(sku, InventoryReservationUnit.STATUS_AVAILABLE);
        long reserved = unitRepository.countBySkuAndStatus(sku, InventoryReservationUnit.STATUS_RESERVED);
        long claimed = unitRepository.countBySkuAndStatus(sku, InventoryReservationUnit.STATUS_CLAIMED);
        long total = unitRepository.countBySku(sku);

        InventoryItem item = inventoryRepository.findBySku(sku).orElse(null);
        int masterStock = item != null ? item.getQuantity() : 0;

        return new PoolStatusDto(sku, masterStock, available, reserved, claimed, total);
    }

    public record ReservationResult(
            String reservationId,
            String sku,
            int quantityReserved,
            String status,
            Instant expiresAt,
            String message
    ) {}

    public record PoolStatusDto(
            String sku,
            int masterStock,
            long availableUnits,
            long reservedUnits,
            long claimedUnits,
            long totalUnitsInPool
    ) {}
}
