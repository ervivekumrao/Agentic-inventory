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

package com.smartinventory.integration.inventory;

import com.smartinventory.inventory.InventoryApplication;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.InventoryReservation;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.InventoryReservationRepository;
import com.smartinventory.inventory.repository.InventoryReservationUnitRepository;
import com.smartinventory.inventory.service.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = InventoryApplication.class)
class InventoryReservationConcurrencyIntegrationTest {

    @Autowired
    private InventoryReservationService reservationService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationUnitRepository unitRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    private static final String CONCURRENT_SKU = "CONCURRENT-SKU-001";

    @BeforeEach
    void setUp() {
        unitRepository.deleteAll();
        reservationRepository.deleteAll();
        inventoryRepository.findBySku(CONCURRENT_SKU).ifPresent(inventoryRepository::delete);

        // Seed master item with 30 units
        InventoryItem item = new InventoryItem(CONCURRENT_SKU, 30, 5, "Warehouse-Main");
        inventoryRepository.save(item);

        // Seed unit pool with 30 units
        List<InventoryReservationUnit> units = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            units.add(new InventoryReservationUnit(CONCURRENT_SKU));
        }
        unitRepository.saveAll(units);
    }

    @Test
    @DisplayName("50 concurrent buyers competing for 30 units must yield exactly 30 reservations and zero overselling")
    void testConcurrentReservations_zeroOverselling() throws Exception {
        int buyers = 50;
        ExecutorService executor = Executors.newFixedThreadPool(buyers);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(buyers);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < buyers; i++) {
            final String resId = "CONC-ORD-" + UUID.randomUUID();
            executor.submit(() -> {
                try {
                    startLatch.await();
                    reservationService.reserveStock(CONCURRENT_SKU, 1, resId, 60);
                    successes.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(30, successes.get(), "Must successfully reserve exactly 30 units");
        assertEquals(20, failures.get(), "Must reject exactly 20 requests due to insufficient stock");

        long reservedCount = unitRepository.countBySkuAndStatus(CONCURRENT_SKU, InventoryReservationUnit.STATUS_RESERVED);
        long availableCount = unitRepository.countBySkuAndStatus(CONCURRENT_SKU, InventoryReservationUnit.STATUS_AVAILABLE);

        assertEquals(30, reservedCount);
        assertEquals(0, availableCount);
    }

    @Test
    @DisplayName("Duplicate claim callback must be idempotent and master stock must decrement exactly once")
    void testClaimReservation_strictIdempotency() {
        String resId = "IDEMP-RES-001";
        var resResult = reservationService.reserveStock(CONCURRENT_SKU, 5, resId, 120);
        assertNotNull(resResult);

        // First claim
        boolean firstClaim = reservationService.claimReservation(resId);
        assertTrue(firstClaim, "First claim must succeed");

        InventoryItem afterFirst = inventoryRepository.findBySku(CONCURRENT_SKU).orElseThrow();
        assertEquals(25, afterFirst.getQuantity(), "Stock must decrement by 5 from 30 to 25");

        // Duplicate replay of claim
        boolean secondClaim = reservationService.claimReservation(resId);
        assertTrue(secondClaim, "Duplicate claim replay must return true idempotently");

        InventoryItem afterSecond = inventoryRepository.findBySku(CONCURRENT_SKU).orElseThrow();
        assertEquals(25, afterSecond.getQuantity(), "Stock must not be decremented twice");
    }

    @Test
    @DisplayName("Conflicting reservationId reuse with different SKU or quantity must be rejected")
    void testReservationIdConflictRejection() {
        String resId = "REUSE-RES-001";
        reservationService.reserveStock(CONCURRENT_SKU, 2, resId, 60);

        // Attempting to reuse same reservationId with different quantity
        assertThrows(IllegalStateException.class, () ->
                reservationService.reserveStock(CONCURRENT_SKU, 5, resId, 60));
    }
}
