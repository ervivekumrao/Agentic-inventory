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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private QueryDialect queryDialect;

    @Test
    @WithMockUser
    void getAllInventory_shouldReturnList() throws Exception {
        InventoryItem item = new InventoryItem("SKU-001", 100, 10, "WH-1");
        item.setId(1L);
        when(inventoryService.getAllInventory()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-001"));
    }

    @Test
    @WithMockUser
    void getInventoryBySku_shouldReturnItem_whenExists() throws Exception {
        InventoryItem item = new InventoryItem("SKU-001", 100, 10, "WH-1");
        item.setId(1L);
        when(inventoryService.getInventoryBySku("SKU-001")).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/inventory/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    @WithMockUser
    void getInventoryBySku_shouldReturnNotFound_whenNotExists() throws Exception {
        when(inventoryService.getInventoryBySku("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/inventory/UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createInventoryItem_shouldReturnCreated() throws Exception {
        InventoryItem item = new InventoryItem("SKU-002", 50, 5, "WH-2");
        item.setId(2L);
        when(inventoryService.createInventoryItem(any(InventoryItem.class))).thenReturn(item);

        mockMvc.perform(post("/api/inventory")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-002"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adjustStock_shouldReturnUpdated() throws Exception {
        InventoryItem item = new InventoryItem("SKU-001", 60, 10, "WH-1");
        item.setId(1L);
        when(inventoryService.adjustStock(eq("SKU-001"), eq(10), any(String.class), any(String.class))).thenReturn(item);

        Map<String, Object> req = Map.of(
                "quantityChange", 10,
                "transactionType", "IN",
                "referenceId", "PO-99"
        );

        mockMvc.perform(post("/api/inventory/SKU-001/adjust")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(60));
    }

    @Test
    @WithMockUser
    void getTransactions_shouldReturnList() throws Exception {
        StockTransaction tx = new StockTransaction("SKU-001", 10, "IN", "PO-99");
        when(inventoryService.getTransactionsForSku("SKU-001")).thenReturn(List.of(tx));

        mockMvc.perform(get("/api/inventory/SKU-001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referenceId").value("PO-99"));
    }

    @Test
    @WithMockUser
    void getDatabaseInfo_shouldReturnMetadata() throws Exception {
        when(queryDialect.getDatabaseMetadata()).thenReturn(Map.of("databaseType", "POSTGRESQL"));

        mockMvc.perform(get("/api/inventory/database-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseType").value("POSTGRESQL"));
    }
}
