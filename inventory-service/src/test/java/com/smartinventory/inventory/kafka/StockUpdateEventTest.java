package com.smartinventory.inventory.kafka;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StockUpdateEventTest {

    @Test
    void testStockUpdateEventGettersAndSetters() {
        StockUpdateEvent event = new StockUpdateEvent();
        event.setSku("SKU-100");
        event.setQuantity(50);
        event.setEventType("ADJUSTMENT");
        Instant now = Instant.now();
        event.setTimestamp(now);

        assertEquals("SKU-100", event.getSku());
        assertEquals(50, event.getQuantity());
        assertEquals("ADJUSTMENT", event.getEventType());
        assertEquals(now, event.getTimestamp());
    }

    @Test
    void testStockUpdateEventConstructor() {
        StockUpdateEvent event = new StockUpdateEvent("SKU-200", 150, "IN");
        assertEquals("SKU-200", event.getSku());
        assertEquals(150, event.getQuantity());
        assertEquals("IN", event.getEventType());
        assertNotNull(event.getTimestamp());
    }
}
