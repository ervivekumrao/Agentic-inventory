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
