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

package com.smartinventory.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventory.kafka.KafkaProducerService;
import com.smartinventory.inventory.kafka.OutboxRelayService;
import com.smartinventory.inventory.kafka.StockUpdateEvent;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.model.OutboxEvent;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.InventoryReservationRepository;
import com.smartinventory.inventory.repository.InventoryReservationUnitRepository;
import com.smartinventory.inventory.repository.OutboxEventRepository;
import com.smartinventory.inventory.service.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ChaosResilienceTest {

    @Autowired
    private InventoryReservationService reservationService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryReservationUnitRepository unitRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private KafkaProducerService kafkaProducerService;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private KafkaTemplate<String, StockUpdateEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        unitRepository.deleteAll();
        reservationRepository.deleteAll();
        outboxEventRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    @DisplayName("Chaos Test 1: Concurrent Duplicate Claim Callbacks - Deducts Stock Exactly Once")
    void testConcurrentDuplicateClaims_deductsOnce() throws Exception {
        String sku = "CHAOS-LAPTOP-01";
        inventoryRepository.save(new InventoryItem(sku, 100, 10, "Warehouse-North"));

        // Reserve 5 units
        String resId = "RES-CHAOS-" + UUID.randomUUID();
        var resResult = reservationService.reserveStock(sku, 5, resId, 300);
        assertEquals(resId, resResult.reservationId());

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean success = false;
                    try {
                        success = reservationService.claimReservation(resId);
                    } catch (org.springframework.dao.ConcurrencyFailureException ex) {
                        // Concurrent claim race: winning thread committed claim, retry returns idempotent true
                        success = reservationService.claimReservation(resId);
                    }
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // All concurrent claim calls should return true (idempotent success)
        assertEquals(threads, successCount.get(), "All duplicate claim attempts must succeed idempotently");

        // Verify master inventory was deducted EXACTLY ONCE (from 100 to 95)
        InventoryItem item = inventoryRepository.findBySku(sku).orElseThrow();
        assertEquals(95, item.getQuantity(), "Master stock must be deducted exactly once by quantity 5");
    }

    @Test
    @DisplayName("Chaos Test 2: Kafka Broker Outage - Outbox Event Isolation & Recovery")
    void testOutboxRelay_kafkaOutageRecovery() {
        // Mock KafkaTemplate to throw an exception simulating broker down
        CompletableFuture<SendResult<String, StockUpdateEvent>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka Broker Connection Refused"));
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(failedFuture);

        // Save a pending outbox event
        OutboxEvent event = new OutboxEvent("StockItem", "SKU-OUTAGE-01", "STOCK_CLAIMED", "{\"sku\":\"SKU-OUTAGE-01\",\"quantity\":50}");
        outboxEventRepository.save(event);

        // Run outbox relay
        outboxRelayService.relayPendingEvents();

        // Verify outbox event transitions to FAILED status without crashing or losing data
        OutboxEvent processed = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxEvent.STATUS_FAILED, processed.getStatus());

        // Now mock KafkaTemplate recovery (successful send)
        CompletableFuture<SendResult<String, StockUpdateEvent>> successFuture = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(successFuture);

        // Re-run outbox relay to simulate background retry upon Kafka recovery
        outboxRelayService.relayPendingEvents();

        // Verify outbox event recovered to SENT status
        OutboxEvent recovered = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxEvent.STATUS_SENT, recovered.getStatus());
    }

    @Test
    @DisplayName("Chaos Test 3: Auto-Reaper Reclamation of Expired Units under Load")
    void testAutoReaper_reclaimsOnlyExpiredUnits() {
        String sku = "CHAOS-REAPER-SKU";
        inventoryRepository.save(new InventoryItem(sku, 50, 5, "Warehouse-Central"));

        // Create 2 expired units and 2 active units
        InventoryReservationUnit unitExpired1 = new InventoryReservationUnit(sku);
        unitExpired1.reserve("RES-EXPIRED-1", Instant.now().minusSeconds(600)); // expired 10 min ago

        InventoryReservationUnit unitExpired2 = new InventoryReservationUnit(sku);
        unitExpired2.reserve("RES-EXPIRED-2", Instant.now().minusSeconds(300)); // expired 5 min ago

        InventoryReservationUnit unitActive = new InventoryReservationUnit(sku);
        unitActive.reserve("RES-ACTIVE", Instant.now().plusSeconds(600)); // active for 10 more min

        unitRepository.saveAll(List.of(unitExpired1, unitExpired2, unitActive));

        // Run cleanup reaper
        reservationService.cleanupExpiredReservations();

        // Verify expired units returned to AVAILABLE, active unit remains RESERVED
        List<InventoryReservationUnit> availableUnits = unitRepository.findBySkuAndStatus(sku, InventoryReservationUnit.STATUS_AVAILABLE);
        assertEquals(2, availableUnits.size(), "Two expired units must be reclaimed to AVAILABLE status");

        List<InventoryReservationUnit> reservedUnits = unitRepository.findBySkuAndStatus(sku, InventoryReservationUnit.STATUS_RESERVED);
        assertEquals(1, reservedUnits.size(), "Active non-expired reservation must remain in RESERVED status");
        assertEquals("RES-ACTIVE", reservedUnits.get(0).getReservationId());
    }
}
