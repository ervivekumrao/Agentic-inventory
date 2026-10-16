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

package com.smartinventory.catalog.controller;

import com.smartinventory.catalog.model.Product;
import com.smartinventory.catalog.security.xss.XssDetector;
import com.smartinventory.catalog.security.xss.XssSanitizer;
import com.smartinventory.catalog.service.CatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(@RequestParam(required = false) String category) {
        log.debug("GET /api/catalog requested with category filter: {}", category != null ? XssSanitizer.sanitize(category) : null);
        if (category != null && !category.isEmpty()) {
            XssDetector.validateInput(category, "category");
            String sanitizedCategory = XssSanitizer.sanitize(category);
            List<Product> products = catalogService.getProductsByCategory(sanitizedCategory);
            if (products.isEmpty() && !sanitizedCategory.equals(category.trim())) {
                products = catalogService.getProductsByCategory(category.trim());
            }
            log.trace("Found {} products for category: {}", products.size(), sanitizedCategory);
            return ResponseEntity.ok(products);
        }
        List<Product> products = catalogService.getAllProducts();
        log.trace("Found {} total products in catalog", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        XssDetector.validateInput(id, "id");
        String sanitizedId = XssSanitizer.sanitize(id);
        log.debug("GET /api/catalog/{} requested", sanitizedId);
        return catalogService.getProductById(sanitizedId)
                .map(product -> {
                    log.trace("Product found for id {}: SKU {}", sanitizedId, product.getSku());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.debug("Product not found for id: {}", sanitizedId);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        XssDetector.validateInput(sku, "sku");
        String sanitizedSku = XssSanitizer.sanitize(sku);
        log.debug("GET /api/catalog/sku/{} requested", sanitizedSku);
        return catalogService.getProductBySku(sanitizedSku)
                .map(product -> {
                    log.trace("Product found for SKU {}: name={}", sanitizedSku, product.getName());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.debug("Product not found for SKU: {}", sanitizedSku);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        XssSanitizer.validateProduct(product);
        Product sanitized = XssSanitizer.sanitizeProduct(product);
        log.info("POST /api/catalog requested for SKU: {}, Name: {}", sanitized.getSku(), sanitized.getName());
        Product created = catalogService.createProduct(sanitized);
        Product safeResponse = XssSanitizer.sanitizeProduct(created);
        log.info("Product created successfully with ID: {}, SKU: {}", safeResponse.getId(), safeResponse.getSku());
        return ResponseEntity.status(HttpStatus.CREATED).body(safeResponse);
    }

    @PostMapping(value = "/bulk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Product>> createProductsBulk(@RequestBody List<Product> products) {
        log.info("POST /api/catalog/bulk requested with {} products", products != null ? products.size() : 0);
        if (products != null) {
            for (Product p : products) {
                XssSanitizer.validateProduct(p);
            }
        }
        List<Product> sanitized = XssSanitizer.sanitizeProducts(products);
        List<Product> created = catalogService.createProductsBulk(sanitized);
        List<Product> safeResponse = XssSanitizer.sanitizeProducts(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(safeResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody Product productDetails) {
        XssDetector.validateInput(id, "id");
        String sanitizedId = XssSanitizer.sanitize(id);
        XssSanitizer.validateProduct(productDetails);
        Product sanitizedDetails = XssSanitizer.sanitizeProduct(productDetails);
        log.info("PUT /api/catalog/{} requested for SKU: {}", sanitizedId, sanitizedDetails.getSku());
        try {
            Product updated = catalogService.updateProduct(sanitizedId, sanitizedDetails);
            Product safeResponse = XssSanitizer.sanitizeProduct(updated);
            log.info("Product updated successfully for ID: {}", sanitizedId);
            return ResponseEntity.ok(safeResponse);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update product for ID: {}. Reason: {}", sanitizedId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        XssDetector.validateInput(id, "id");
        String sanitizedId = XssSanitizer.sanitize(id);
        log.info("DELETE /api/catalog/{} requested", sanitizedId);
        try {
            catalogService.deleteProduct(sanitizedId);
            log.info("Product deleted successfully for ID: {}", sanitizedId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete product for ID: {}. Reason: {}", sanitizedId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
