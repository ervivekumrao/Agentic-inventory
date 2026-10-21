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
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.InventoryReservationUnitRepository;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryReservationUnitRepository unitRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private InventoryReservationService reservationService;

    private InventoryReservationUnit unit1;
    private InventoryReservationUnit unit2;
    private InventoryItem masterItem;

    @BeforeEach
    void setUp() {
        unit1 = new InventoryReservationUnit("SKU-TEST");
        unit1.setId(101L);

        unit2 = new InventoryReservationUnit("SKU-TEST");
        unit2.setId(102L);

        masterItem = new InventoryItem("SKU-TEST", 50, 5, "Warehouse-A");
    }

    @Test
    void reserveStock_shouldSucceed_whenAvailableUnitsExist() {
        when(unitRepository.findByReservationId("RES-123")).thenReturn(List.of());
        when(unitRepository.fetchAvailableUnitsForUpdate("SKU-TEST", 2)).thenReturn(List.of(unit1, unit2));

        var result = reservationService.reserveStock("SKU-TEST", 2, "RES-123", 10);

        assertNotNull(result);
        assertEquals(2, result.quantityReserved());
        assertEquals("RES-123", result.reservationId());
        assertEquals("SKU-TEST", result.sku());
        assertEquals(InventoryReservationUnit.STATUS_RESERVED, unit1.getStatus());
        assertEquals("RES-123", unit1.getReservationId());
        assertNotNull(unit1.getExpiresAt());
        verify(unitRepository, times(1)).saveAll(anyList());
    }

    @Test
    void reserveStock_idempotency_shouldReturnExistingReservation() {
        unit1.reserve("RES-EXISTING", Instant.now().plusSeconds(600));

        when(unitRepository.findByReservationId("RES-EXISTING")).thenReturn(List.of(unit1));

        var result = reservationService.reserveStock("SKU-TEST", 1, "RES-EXISTING", 10);

        assertNotNull(result);
        assertEquals(1, result.quantityReserved());
        assertEquals("RES-EXISTING", result.reservationId());
        verify(unitRepository, never()).fetchAvailableUnitsForUpdate(anyString(), anyInt());
    }

    @Test
    void reserveStock_shouldThrowException_whenInsufficientUnits() {
        when(unitRepository.findByReservationId("RES-EMPTY")).thenReturn(List.of());
        when(unitRepository.fetchAvailableUnitsForUpdate("SKU-TEST", 5)).thenReturn(List.of());
        when(inventoryRepository.findBySku("SKU-TEST")).thenReturn(Optional.of(new InventoryItem("SKU-TEST", 0, 5, "Warehouse-A")));

        assertThrows(IllegalArgumentException.class, () ->
                reservationService.reserveStock("SKU-TEST", 5, "RES-EMPTY", 10));
    }

    @Test
    void claimReservation_shouldTransitionReservedToClaimed() {
        unit1.reserve("RES-CLAIM", Instant.now().plusSeconds(300));

        when(unitRepository.findByReservationId("RES-CLAIM")).thenReturn(List.of(unit1));
        when(inventoryRepository.findBySku("SKU-TEST")).thenReturn(Optional.of(masterItem));

        boolean claimed = reservationService.claimReservation("RES-CLAIM");

        assertTrue(claimed);
        assertEquals(InventoryReservationUnit.STATUS_CLAIMED, unit1.getStatus());
        verify(transactionRepository, times(1)).save(any());
        verify(kafkaProducerService, times(1)).sendStockUpdateEvent(any());
    }

    @Test
    void releaseReservation_shouldTransitionBackToAvailable() {
        unit1.reserve("RES-CANCEL", Instant.now().plusSeconds(300));

        when(unitRepository.findByReservationId("RES-CANCEL")).thenReturn(List.of(unit1));

        boolean released = reservationService.releaseReservation("RES-CANCEL");

        assertTrue(released);
        assertEquals(InventoryReservationUnit.STATUS_AVAILABLE, unit1.getStatus());
        assertNull(unit1.getReservationId());
        assertNull(unit1.getExpiresAt());
        verify(unitRepository, times(1)).saveAll(anyList());
    }

    @Test
    void cleanupExpiredReservations_shouldReleaseExpiredUnits() {
        unit1.reserve("RES-EXPIRED", Instant.now().minusSeconds(10));

        when(unitRepository.findByStatusAndExpiresAtBefore(eq(InventoryReservationUnit.STATUS_RESERVED), any(Instant.class)))
                .thenReturn(List.of(unit1));

        reservationService.cleanupExpiredReservations();

        assertEquals(InventoryReservationUnit.STATUS_AVAILABLE, unit1.getStatus());
        assertNull(unit1.getReservationId());
        verify(unitRepository, times(1)).saveAll(anyList());
    }
}
