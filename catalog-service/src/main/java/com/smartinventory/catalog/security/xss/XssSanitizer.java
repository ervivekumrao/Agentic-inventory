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
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sanitization utility for neutralizing potential Cross-Site Scripting (XSS) vectors.
 */
public final class XssSanitizer {

    private XssSanitizer() {}

    /**
     * Sanitizes untrusted input string using HTML entity encoding.
     *
     * @param input raw untrusted string
     * @return HTML-escaped string, or original if null
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(input.trim());
    }

    /**
     * Sanitizes all string fields and attributes in a {@link Product}.
     *
     * @param product product entity to sanitize
     * @return sanitized product entity
     */
    public static Product sanitizeProduct(Product product) {
        if (product == null) {
            return null;
        }
        if (product.getId() != null) {
            product.setId(sanitize(product.getId()));
        }
        if (product.getSku() != null) {
            product.setSku(sanitize(product.getSku()));
        }
        if (product.getName() != null) {
            product.setName(sanitize(product.getName()));
        }
        if (product.getDescription() != null) {
            product.setDescription(sanitize(product.getDescription()));
        }
        if (product.getCategory() != null) {
            product.setCategory(sanitize(product.getCategory()));
        }
        if (product.getAttributes() != null) {
            Map<String, String> sanitizedAttributes = new HashMap<>();
            product.getAttributes().forEach((k, v) -> {
                String safeKey = sanitize(k);
                String safeVal = sanitize(v);
                sanitizedAttributes.put(safeKey, safeVal);
            });
            product.setAttributes(sanitizedAttributes);
        }
        return product;
    }

    /**
     * Sanitizes a list of {@link Product} objects.
     *
     * @param products list of products to sanitize
     * @return sanitized list of products
     */
    public static List<Product> sanitizeProducts(List<Product> products) {
        if (products == null) {
            return List.of();
        }
        List<Product> result = new ArrayList<>(products.size());
        for (Product p : products) {
            result.add(sanitizeProduct(p));
        }
        return result;
    }

    /**
     * Validates that all string fields and attributes of a product do not contain XSS attack vectors.
     * Throws {@link XssException} if an attack is detected.
     *
     * @param product product entity to validate
     */
    public static void validateProduct(Product product) {
        if (product == null) {
            return;
        }
        XssDetector.validateInput(product.getId(), "id");
        XssDetector.validateInput(product.getSku(), "sku");
        XssDetector.validateInput(product.getName(), "name");
        XssDetector.validateInput(product.getDescription(), "description");
        XssDetector.validateInput(product.getCategory(), "category");
        if (product.getAttributes() != null) {
            product.getAttributes().forEach((key, val) -> {
                XssDetector.validateInput(key, "attributes.key");
                XssDetector.validateInput(val, "attributes.value");
            });
        }
    }
}
