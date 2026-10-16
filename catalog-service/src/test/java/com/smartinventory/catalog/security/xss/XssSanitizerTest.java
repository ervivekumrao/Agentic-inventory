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

package com.smartinventory.catalog.security.xss;

import com.smartinventory.catalog.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XssSanitizerTest {

    @Test
    void sanitize_shouldEscapeHtmlCharacters() {
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", XssSanitizer.sanitize("<script>alert(1)</script>"));
        assertEquals("Safe Product", XssSanitizer.sanitize("  Safe Product  "));
        assertNull(XssSanitizer.sanitize(null));
    }

    @Test
    void sanitizeProduct_shouldSanitizeAllFields() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("color", "<b>Red</b>");
        Product product = new Product("SKU-1", "<b>Phone</b>", "<i>Best phone</i>", "<span>Electronics</span>", BigDecimal.valueOf(100.0), attributes);
        product.setId("<id>");

        Product sanitized = XssSanitizer.sanitizeProduct(product);

        assertNotNull(sanitized);
        assertEquals("&lt;id&gt;", sanitized.getId());
        assertEquals("SKU-1", sanitized.getSku());
        assertEquals("&lt;b&gt;Phone&lt;/b&gt;", sanitized.getName());
        assertEquals("&lt;i&gt;Best phone&lt;/i&gt;", sanitized.getDescription());
        assertEquals("&lt;span&gt;Electronics&lt;/span&gt;", sanitized.getCategory());
        assertEquals("&lt;b&gt;Red&lt;/b&gt;", sanitized.getAttributes().get("color"));
    }

    @Test
    void sanitizeProduct_shouldHandleNullProductAndNullFields() {
        assertNull(XssSanitizer.sanitizeProduct(null));

        Product emptyProduct = new Product();
        Product sanitized = XssSanitizer.sanitizeProduct(emptyProduct);
        assertNotNull(sanitized);
        assertNull(sanitized.getSku());
        assertNull(sanitized.getName());
        assertNull(sanitized.getDescription());
        assertNull(sanitized.getCategory());
        assertNull(sanitized.getAttributes());
    }

    @Test
    void sanitizeProducts_shouldSanitizeAllElementsInList() {
        Product p1 = new Product("SKU-1", "<b>Item 1</b>", null, "Cat", BigDecimal.TEN, null);
        Product p2 = new Product("SKU-2", "<i>Item 2</i>", null, "Cat", BigDecimal.ONE, null);

        List<Product> sanitizedList = XssSanitizer.sanitizeProducts(List.of(p1, p2));

        assertEquals(2, sanitizedList.size());
        assertEquals("&lt;b&gt;Item 1&lt;/b&gt;", sanitizedList.get(0).getName());
        assertEquals("&lt;i&gt;Item 2&lt;/i&gt;", sanitizedList.get(1).getName());

        assertTrue(XssSanitizer.sanitizeProducts(null).isEmpty());
    }

    @Test
    void validateProduct_shouldThrowException_whenMaliciousInputPresent() {
        Product maliciousProduct = new Product("SKU-MALICIOUS", "Normal Name", "<script>alert(1)</script>", "Electronics", BigDecimal.valueOf(99.9), null);

        assertThrows(XssException.class, () -> XssSanitizer.validateProduct(maliciousProduct));
    }

    @Test
    void validateProduct_shouldPass_whenSafeInputProvided() {
        Product safeProduct = new Product("SKU-001", "Keyboard", "Wireless keyboard", "Electronics", BigDecimal.valueOf(49.99), Map.of("color", "Black"));

        assertDoesNotThrow(() -> XssSanitizer.validateProduct(safeProduct));
    }
}
