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
        log.debug("GET /api/catalog requested with category filter: {}", category);
        if (category != null && !category.isEmpty()) {
            List<Product> products = catalogService.getProductsByCategory(category);
            log.trace("Found {} products for category: {}", products.size(), category);
            return ResponseEntity.ok(products);
        }
        List<Product> products = catalogService.getAllProducts();
        log.trace("Found {} total products in catalog", products.size());
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        log.debug("GET /api/catalog/{} requested", id);
        return catalogService.getProductById(id)
                .map(product -> {
                    log.trace("Product found for id {}: SKU {}", id, product.getSku());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.debug("Product not found for id: {}", id);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        log.debug("GET /api/catalog/sku/{} requested", sku);
        return catalogService.getProductBySku(sku)
                .map(product -> {
                    log.trace("Product found for SKU {}: name={}", sku, product.getName());
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> {
                    log.debug("Product not found for SKU: {}", sku);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                });
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        log.info("POST /api/catalog requested for SKU: {}, Name: {}", product.getSku(), product.getName());
        Product created = catalogService.createProduct(product);
        log.info("Product created successfully with ID: {}, SKU: {}", created.getId(), created.getSku());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping(value = "/bulk", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Product>> createProductsBulk(@RequestBody List<Product> products) {
        log.info("POST /api/catalog/bulk requested with {} products", products.size());
        List<Product> created = catalogService.createProductsBulk(products);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @Valid @RequestBody Product productDetails) {
        log.info("PUT /api/catalog/{} requested for SKU: {}", id, productDetails.getSku());
        try {
            Product updated = catalogService.updateProduct(id, productDetails);
            log.info("Product updated successfully for ID: {}", id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update product for ID: {}. Reason: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("DELETE /api/catalog/{} requested", id);
        try {
            catalogService.deleteProduct(id);
            log.info("Product deleted successfully for ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete product for ID: {}. Reason: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
