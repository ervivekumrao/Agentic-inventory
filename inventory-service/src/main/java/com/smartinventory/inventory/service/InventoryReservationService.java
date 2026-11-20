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
import com.smartinventory.inventory.model.InventoryReservation;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.InventoryReservationRepository;
import com.smartinventory.inventory.repository.InventoryReservationUnitRepository;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * High-Scale Inventory Reservation Service implementing a bounded unit-pool
 * non-blocking locking architecture (FOR UPDATE SKIP LOCKED).
 *
 * Eliminates database hot-row serialization and lock wait timeouts during peak flash sales.
 */
@Service
public class InventoryReservationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationService.class);
    public static final int DEFAULT_MAX_POOL_SIZE = 500;
    public static final int DEFAULT_TTL_SECONDS = 600; // 10 minutes
    public static final int MAX_TTL_SECONDS = 900; // 15 minutes max
    public static final int MAX_RESERVATION_QUANTITY = 50;

    private final InventoryReservationUnitRepository unitRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public InventoryReservationService(
            InventoryReservationUnitRepository unitRepository,
            InventoryReservationRepository reservationRepository,
            InventoryRepository inventoryRepository,
            StockTransactionRepository transactionRepository,
            KafkaProducerService kafkaProducerService,
            @Autowired(required = false) MeterRegistry meterRegistry) {
        this.unitRepository = unitRepository;
        this.reservationRepository = reservationRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaProducerService = kafkaProducerService;
        this.meterRegistry = meterRegistry;
    }

    public InventoryReservationService(
            InventoryReservationUnitRepository unitRepository,
            InventoryReservationRepository reservationRepository,
            InventoryRepository inventoryRepository,
            StockTransactionRepository transactionRepository,
            KafkaProducerService kafkaProducerService) {
        this(unitRepository, reservationRepository, inventoryRepository, transactionRepository, kafkaProducerService, null);
    }

    private void recordCounter(String metricName, String... tags) {
        if (meterRegistry != null) {
            try {
                meterRegistry.counter(metricName, tags).increment();
            } catch (Exception ignored) {}
        }
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
        if (quantity > MAX_RESERVATION_QUANTITY) {
            throw new IllegalArgumentException("Reservation quantity exceeds maximum limit of " + MAX_RESERVATION_QUANTITY);
        }

        String reservationId = (clientReservationId != null && !clientReservationId.isBlank())
                ? clientReservationId.trim()
                : UUID.randomUUID().toString();

        // 1. Authoritative Header Check: if reservation exists, enforce idempotency or conflict
        Optional<InventoryReservation> existingHeader = reservationRepository.findById(reservationId);
        if (existingHeader.isPresent()) {
            InventoryReservation res = existingHeader.get();
            if (!res.getSku().equalsIgnoreCase(sku) || res.getQuantity() != quantity) {
                log.warn("Reservation ID {} reused with conflicting SKU/quantity: original={}:{}, requested={}:{}",
                        reservationId, res.getSku(), res.getQuantity(), sku, quantity);
                throw new IllegalStateException("Reservation ID " + reservationId + " already exists with conflicting parameters");
            }
            log.info("Idempotent reservation replay detected for reservationId: {}, status: {}", reservationId, res.getStatus());
            recordCounter("inventory.reservation.requests", "status", "REPLAYED", "sku", res.getSku());
            return new ReservationResult(
                    reservationId,
                    res.getSku(),
                    res.getQuantity(),
                    res.getStatus(),
                    res.getExpiresAt(),
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
            recordCounter("inventory.reservation.requests", "status", "INSUFFICIENT_STOCK", "sku", sku);
            throw new IllegalArgumentException("Insufficient inventory available to reserve " + quantity + " units for SKU: " + sku);
        }

        int ttl = (ttlSeconds != null && ttlSeconds > 0) ? Math.min(ttlSeconds, MAX_TTL_SECONDS) : DEFAULT_TTL_SECONDS;
        Instant expiresAt = Instant.now().plusSeconds(ttl);

        for (InventoryReservationUnit unit : lockedUnits) {
            unit.reserve(reservationId, expiresAt);
        }
        unitRepository.saveAll(lockedUnits);

        InventoryReservation header = new InventoryReservation(reservationId, sku, quantity, expiresAt);
        reservationRepository.save(header);

        log.info("Successfully reserved {} units for SKU: {} with reservationId: {} expiring at: {}",
                lockedUnits.size(), sku, reservationId, expiresAt);
        recordCounter("inventory.reservation.requests", "status", "SUCCESS", "sku", sku);

        return new ReservationResult(
                reservationId,
                sku,
                lockedUnits.size(),
                InventoryReservation.STATUS_PENDING_PAYMENT,
                expiresAt,
                "Stock successfully reserved with non-blocking SKIP LOCKED"
        );
    }

    /**
     * Claims a previously reserved batch upon successful payment completion.
     * Permanently decrements the master ledger atomically and records the stock transaction.
     * Strictly idempotent: replayed callbacks return true without duplicate deductions.
     */
    @Transactional
    public boolean claimReservation(String reservationId) {
        Optional<InventoryReservation> headerOpt = reservationRepository.findById(reservationId);
        if (headerOpt.isPresent() && InventoryReservation.STATUS_COMPLETED.equals(headerOpt.get().getStatus())) {
            log.info("Reservation {} already claimed. Idempotent success returned.", reservationId);
            recordCounter("inventory.reservation.claims", "status", "ALREADY_CLAIMED");
            return true;
        }

        List<InventoryReservationUnit> units = unitRepository.findByReservationId(reservationId);
        if (units.isEmpty()) {
            log.warn("Cannot claim reservation: no units found for ID: {}", reservationId);
            recordCounter("inventory.reservation.claims", "status", "FAILED");
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
            // Check if all units are already claimed
            boolean allClaimed = units.stream().allMatch(u -> InventoryReservationUnit.STATUS_CLAIMED.equals(u.getStatus()));
            if (allClaimed) {
                log.info("All units for reservationId {} already claimed. Idempotent success.", reservationId);
                recordCounter("inventory.reservation.claims", "status", "ALREADY_CLAIMED");
                return true;
            }
            log.warn("No active RESERVED units to claim for reservationId: {}", reservationId);
            recordCounter("inventory.reservation.claims", "status", "FAILED");
            return false;
        }

        unitRepository.saveAll(units);

        // Atomic deduction from master ledger (avoids read-modify-write lost updates and negative stock masking)
        int updatedRows = inventoryRepository.decrementStockIfSufficient(sku, count);
        if (updatedRows == 0) {
            log.error("Failed to decrement master stock for SKU: {} by count: {}. Insufficient stock on hand!", sku, count);
            recordCounter("inventory.reservation.claims", "status", "FAILED");
            throw new IllegalStateException("Insufficient master stock to claim reserved units for SKU: " + sku);
        }

        InventoryItem item = inventoryRepository.findBySku(sku).orElse(null);
        int newQty = item != null ? item.getQuantity() : 0;
        transactionRepository.save(new StockTransaction(sku, -count, "RESERVATION_CLAIM", reservationId));
        kafkaProducerService.sendStockUpdateEvent(new StockUpdateEvent(sku, newQty, "RESERVATION_CLAIMED"));

        if (headerOpt.isPresent()) {
            InventoryReservation header = headerOpt.get();
            header.markCompleted("GATEWAY-CALLBACK");
            reservationRepository.save(header);
        }

        log.info("Successfully claimed {} reserved units for SKU: {} (reservationId: {})", count, sku, reservationId);
        recordCounter("inventory.reservation.claims", "status", "SUCCESS", "sku", sku);
        return true;
    }

    /**
     * Releases a reservation (e.g. buyer abandoned cart, payment declined, or cancelled).
     * Units immediately return to AVAILABLE status in the pool without master ledger writes.
     * Strictly idempotent: replayed releases return true.
     */
    @Transactional
    public boolean releaseReservation(String reservationId) {
        Optional<InventoryReservation> headerOpt = reservationRepository.findById(reservationId);
        if (headerOpt.isPresent() && InventoryReservation.STATUS_FAILED.equals(headerOpt.get().getStatus())) {
            log.info("Reservation {} already released/failed. Idempotent success returned.", reservationId);
            return true;
        }

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

        if (count > 0 || (headerOpt.isPresent() && InventoryReservation.STATUS_PENDING_PAYMENT.equals(headerOpt.get().getStatus()))) {
            unitRepository.saveAll(units);
            if (headerOpt.isPresent()) {
                InventoryReservation header = headerOpt.get();
                header.markFailed();
                reservationRepository.save(header);
            }
            log.info("Released {} reserved units back to available pool for reservationId: {}", count, reservationId);
            return true;
        }
        return false;
    }

    /**
     * Automated Reaper: Runs periodically to reclaim expired reservations.
     * Any reservation exceeding its TTL is freed back to AVAILABLE for other buyers.
     * Updates header status to EXPIRED to maintain full audit trail.
     */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void cleanupExpiredReservations() {
        List<InventoryReservationUnit> expiredUnits = unitRepository.findByStatusAndExpiresAtBefore(
                InventoryReservationUnit.STATUS_RESERVED,
                Instant.now()
        );

        if (!expiredUnits.isEmpty()) {
            for (InventoryReservationUnit unit : expiredUnits) {
                unit.release();
            }
            unitRepository.saveAll(expiredUnits);
            log.info("Auto-reaper reclaimed {} expired reservation units back to pool", expiredUnits.size());
        }

        List<InventoryReservation> expiredHeaders = reservationRepository.findByStatusAndExpiresAtBefore(
                InventoryReservation.STATUS_PENDING_PAYMENT,
                Instant.now()
        );
        if (!expiredHeaders.isEmpty()) {
            for (InventoryReservation res : expiredHeaders) {
                res.markExpired();
            }
            reservationRepository.saveAll(expiredHeaders);
            log.info("Auto-reaper marked {} reservation headers as EXPIRED", expiredHeaders.size());
        }
    }

    /**
     * Automated Reaper for Claimed Units:
     * Prunes CLAIMED reservation units older than 1 hour to prevent unbounded table bloat in high-volume environments.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupClaimedReservations() {
        Instant threshold = Instant.now().minus(java.time.Duration.ofHours(1));
        List<InventoryReservationUnit> claimedOld = unitRepository.findByStatusAndCreatedAtBefore(
                InventoryReservationUnit.STATUS_CLAIMED,
                threshold
        );
        if (!claimedOld.isEmpty()) {
            unitRepository.deleteAllInBatch(claimedOld);
            log.info("Auto-reaper purged {} old claimed reservation units to maintain bounded table size", claimedOld.size());
        }
    }

    /**
     * Replenishes the bounded unit pool from the master InventoryItem ledger.
     * Uses non-blocking check followed by pessimistic write lock on the master ledger record
     * to prevent race conditions across distributed cluster pods without JVM-level instance monitors.
     */
    @Transactional
    public void replenishPoolFromMasterLedger(String sku, int minimumNeeded) {
        // Fast-path: Check item without locking first to avoid lock contention on depleted items
        Optional<InventoryItem> checkItem = inventoryRepository.findBySku(sku);
        if (checkItem.isEmpty() || checkItem.get().getQuantity() <= 0) {
            return;
        }

        InventoryItem item = inventoryRepository.findBySkuForUpdate(sku)
                .orElse(null);
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
