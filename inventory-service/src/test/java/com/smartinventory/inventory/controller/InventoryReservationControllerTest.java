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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinventory.inventory.db.dialect.QueryDialect;
import com.smartinventory.inventory.model.InventoryReservationUnit;
import com.smartinventory.inventory.service.InventoryReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryReservationController.class)
class InventoryReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryReservationService reservationService;

    @MockitoBean
    private QueryDialect queryDialect;

    @Test
    @WithMockUser
    void reserveStock_shouldReturn201_whenSuccessful() throws Exception {
        InventoryReservationService.ReservationResult result = new InventoryReservationService.ReservationResult(
                "RES-123",
                "SKU-001",
                2,
                InventoryReservationUnit.STATUS_RESERVED,
                Instant.now().plusSeconds(600),
                "Stock successfully reserved"
        );

        when(reservationService.reserveStock(eq("SKU-001"), eq(2), any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/inventory/reservations/reserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("sku", "SKU-001", "quantity", 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value("RES-123"))
                .andExpect(jsonPath("$.quantityReserved").value(2))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    @WithMockUser
    void claimReservation_shouldReturn200_whenClaimed() throws Exception {
        when(reservationService.claimReservation("RES-123")).thenReturn(true);

        mockMvc.perform(post("/api/inventory/reservations/claim/RES-123").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));
    }

    @Test
    @WithMockUser
    void releaseReservation_shouldReturn200_whenReleased() throws Exception {
        when(reservationService.releaseReservation("RES-123")).thenReturn(true);

        mockMvc.perform(post("/api/inventory/reservations/release/RES-123").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    @WithMockUser
    void getPoolStatus_shouldReturnPoolMetrics() throws Exception {
        InventoryReservationService.PoolStatusDto poolStatus = new InventoryReservationService.PoolStatusDto(
                "SKU-001", 100, 50, 10, 5, 65
        );

        when(reservationService.getPoolStatus("SKU-001")).thenReturn(poolStatus);

        mockMvc.perform(get("/api/inventory/reservations/pool/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.masterStock").value(100))
                .andExpect(jsonPath("$.availableUnits").value(50));
    }
}
