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

package com.smartinventory.inventory.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {

    @Test
    void testInventoryItemGettersAndSetters() {
        InventoryItem item = new InventoryItem();
        item.setId(10L);
        item.setSku("SKU-100");
        item.setQuantity(50);
        item.setReorderThreshold(10);
        item.setWarehouseLocation("WH-NORTH");
        item.setVersion(1L);
        Instant now = Instant.now();
        item.setUpdatedAt(now);

        assertEquals(10L, item.getId());
        assertEquals("SKU-100", item.getSku());
        assertEquals(50, item.getQuantity());
        assertEquals(10, item.getReorderThreshold());
        assertEquals("WH-NORTH", item.getWarehouseLocation());
        assertEquals(1L, item.getVersion());
        assertEquals(now, item.getUpdatedAt());
    }

    @Test
    void testInventoryItemConstructor() {
        InventoryItem item = new InventoryItem("SKU-200", 100, 20, "WH-SOUTH");
        assertEquals("SKU-200", item.getSku());
        assertEquals(100, item.getQuantity());
        assertEquals(20, item.getReorderThreshold());
        assertEquals("WH-SOUTH", item.getWarehouseLocation());
        assertNotNull(item.getUpdatedAt());
    }
}
