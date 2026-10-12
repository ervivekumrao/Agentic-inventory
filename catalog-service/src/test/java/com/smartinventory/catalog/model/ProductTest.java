package com.smartinventory.catalog.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testProductGettersAndSetters() {
        Product product = new Product();
        product.setId("100");
        product.setSku("SKU-TEST");
        product.setName("Test Product");
        product.setDescription("Description");
        product.setCategory("Electronics");
        product.setPrice(BigDecimal.valueOf(99.99));
        Map<String, String> attrs = Map.of("brand", "Smart");
        product.setAttributes(attrs);
        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        assertEquals("100", product.getId());
        assertEquals("SKU-TEST", product.getSku());
        assertEquals("Test Product", product.getName());
        assertEquals("Description", product.getDescription());
        assertEquals("Electronics", product.getCategory());
        assertEquals(BigDecimal.valueOf(99.99), product.getPrice());
        assertEquals(attrs, product.getAttributes());
        assertEquals(now, product.getCreatedAt());
        assertEquals(now, product.getUpdatedAt());
    }

    @Test
    void testProductConstructor() {
        Map<String, String> attrs = Map.of("brand", "Smart");
        Product product = new Product("SKU-CON", "Name", "Desc", "Cat", BigDecimal.valueOf(50.00), attrs);

        assertEquals("SKU-CON", product.getSku());
        assertEquals("Name", product.getName());
        assertEquals("Desc", product.getDescription());
        assertEquals("Cat", product.getCategory());
        assertEquals(BigDecimal.valueOf(50.00), product.getPrice());
        assertEquals(attrs, product.getAttributes());
        assertNotNull(product.getCreatedAt());
        assertNotNull(product.getUpdatedAt());
    }
}
