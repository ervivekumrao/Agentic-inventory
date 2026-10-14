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

package com.smartinventory.catalog.service;

import com.smartinventory.catalog.model.Product;
import com.smartinventory.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CatalogService catalogService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product("SKU-001", "Laptop", "High performance laptop", "Electronics", BigDecimal.valueOf(1200.00), null);
        product.setId("123");
    }

    @Test
    void getAllProducts_shouldReturnList() {
        when(productRepository.findAll()).thenReturn(List.of(product));
        List<Product> products = catalogService.getAllProducts();
        assertEquals(1, products.size());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void getProductById_shouldReturnProduct_whenExists() {
        when(productRepository.findById("123")).thenReturn(Optional.of(product));
        Optional<Product> found = catalogService.getProductById("123");
        assertTrue(found.isPresent());
        assertEquals("SKU-001", found.get().getSku());
    }

    @Test
    void getProductBySku_shouldReturnProduct_whenExists() {
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));
        Optional<Product> found = catalogService.getProductBySku("SKU-001");
        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName());
    }

    @Test
    void getProductsByCategory_shouldReturnList() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(product));
        List<Product> products = catalogService.getProductsByCategory("Electronics");
        assertEquals(1, products.size());
    }

    @Test
    void createProduct_shouldSave_whenSkuNotExists() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product created = catalogService.createProduct(product);
        assertNotNull(created);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowException_whenSkuExists() {
        when(productRepository.existsBySku("SKU-001")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> catalogService.createProduct(product));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_shouldUpdate_whenExists() {
        when(productRepository.findById("123")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product updatedDetails = new Product("SKU-001", "Gaming Laptop", "Updated desc", "Electronics", BigDecimal.valueOf(1500.00), null);
        Product updated = catalogService.updateProduct("123", updatedDetails);

        assertEquals("Gaming Laptop", updated.getName());
        assertEquals(BigDecimal.valueOf(1500.00), updated.getPrice());
    }

    @Test
    void updateProduct_shouldThrowException_whenNotFound() {
        when(productRepository.findById("999")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> catalogService.updateProduct("999", product));
    }

    @Test
    void deleteProduct_shouldDelete_whenExists() {
        when(productRepository.existsById("123")).thenReturn(true);
        doNothing().when(productRepository).deleteById("123");

        assertDoesNotThrow(() -> catalogService.deleteProduct("123"));
        verify(productRepository, times(1)).deleteById("123");
    }

    @Test
    void deleteProduct_shouldThrowException_whenNotFound() {
        when(productRepository.existsById("999")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> catalogService.deleteProduct("999"));
    }
}
