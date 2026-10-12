package com.smartinventory.catalog.controller;

import com.smartinventory.catalog.model.Product;
import com.smartinventory.catalog.service.CatalogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CatalogService catalogService;

    @Test
    void getAllProducts_shouldReturnList() throws Exception {
        Product p = new Product("SKU-001", "Phone", "Smartphone", "Electronics", BigDecimal.valueOf(500.00), null);
        p.setId("1");
        when(catalogService.getAllProducts()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].name").value("Phone"));
    }

    @Test
    void getAllProducts_withCategory_shouldReturnFilteredList() throws Exception {
        Product p = new Product("SKU-001", "Phone", "Smartphone", "Electronics", BigDecimal.valueOf(500.00), null);
        when(catalogService.getProductsByCategory("Electronics")).thenReturn(List.of(p));

        mockMvc.perform(get("/api/catalog").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    void getProductById_shouldReturnProduct_whenExists() throws Exception {
        Product p = new Product("SKU-001", "Phone", "Smartphone", "Electronics", BigDecimal.valueOf(500.00), null);
        p.setId("1");
        when(catalogService.getProductById("1")).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getProductById_shouldReturnNotFound_whenNotExists() throws Exception {
        when(catalogService.getProductById("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductBySku_shouldReturnProduct_whenExists() throws Exception {
        Product p = new Product("SKU-001", "Phone", "Smartphone", "Electronics", BigDecimal.valueOf(500.00), null);
        when(catalogService.getProductBySku("SKU-001")).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/catalog/sku/SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Phone"));
    }

    @Test
    void getProductBySku_shouldReturnNotFound_whenNotExists() throws Exception {
        when(catalogService.getProductBySku("SKU-UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/catalog/sku/SKU-UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_shouldReturnCreated() throws Exception {
        Product p = new Product("SKU-002", "Tablet", "Tab desc", "Electronics", BigDecimal.valueOf(300.00), null);
        p.setId("2");
        when(catalogService.createProduct(any(Product.class))).thenReturn(p);

        mockMvc.perform(post("/api/catalog")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("SKU-002"));
    }

    @Test
    void updateProduct_shouldReturnUpdated_whenExists() throws Exception {
        Product p = new Product("SKU-002", "Tablet Updated", "Tab desc", "Electronics", BigDecimal.valueOf(350.00), null);
        p.setId("2");
        when(catalogService.updateProduct(eq("2"), any(Product.class))).thenReturn(p);

        mockMvc.perform(put("/api/catalog/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tablet Updated"));
    }

    @Test
    void updateProduct_shouldReturnNotFound_whenNotExists() throws Exception {
        Product p = new Product("SKU-999", "Tablet", "Tab desc", "Electronics", BigDecimal.valueOf(300.00), null);
        when(catalogService.updateProduct(eq("999"), any(Product.class))).thenThrow(new IllegalArgumentException("Not found"));

        mockMvc.perform(put("/api/catalog/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProduct_shouldReturnNoContent_whenExists() throws Exception {
        doNothing().when(catalogService).deleteProduct("1");

        mockMvc.perform(delete("/api/catalog/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_shouldReturnNotFound_whenNotExists() throws Exception {
        doThrow(new IllegalArgumentException("Not found")).when(catalogService).deleteProduct("999");

        mockMvc.perform(delete("/api/catalog/999"))
                .andExpect(status().isNotFound());
    }
}
