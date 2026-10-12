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
