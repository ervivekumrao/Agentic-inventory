package com.smartinventory.inventory.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StockTransactionTest {

    @Test
    void testStockTransactionGettersAndSetters() {
        StockTransaction tx = new StockTransaction();
        tx.setId(5L);
        tx.setSku("SKU-100");
        tx.setQuantityChange(25);
        tx.setTransactionType("IN");
        tx.setReferenceId("PO-123");
        Instant now = Instant.now();
        tx.setTimestamp(now);

        assertEquals(5L, tx.getId());
        assertEquals("SKU-100", tx.getSku());
        assertEquals(25, tx.getQuantityChange());
        assertEquals("IN", tx.getTransactionType());
        assertEquals("PO-123", tx.getReferenceId());
        assertEquals(now, tx.getTimestamp());
    }

    @Test
    void testStockTransactionConstructor() {
        StockTransaction tx = new StockTransaction("SKU-200", -5, "OUT", "SO-456");
        assertEquals("SKU-200", tx.getSku());
        assertEquals(-5, tx.getQuantityChange());
        assertEquals("OUT", tx.getTransactionType());
        assertEquals("SO-456", tx.getReferenceId());
        assertNotNull(tx.getTimestamp());
    }
}
