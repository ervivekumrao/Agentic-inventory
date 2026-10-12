package com.smartinventory.inventory.service;

import com.smartinventory.inventory.dto.AiQueryRequest;
import com.smartinventory.inventory.dto.AiQueryResponse;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAgentServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RestClient restClient;

    private AiAgentService aiAgentService;

    @BeforeEach
    void setUp() {
        aiAgentService = new AiAgentService(inventoryRepository, restClient, "http://localhost:8083", "http://localhost:8082");
    }

    @Test
    void processQuery_shouldHydrateContextAndCallFallbackWhenRestClientFails() {
        InventoryItem item1 = new InventoryItem("SKU-001", 3, 10, "Warehouse-North");
        InventoryItem item2 = new InventoryItem("SKU-002", 50, 10, "Warehouse-South");
        when(inventoryRepository.findAll()).thenReturn(List.of(item1, item2));

        AiQueryRequest request = new AiQueryRequest("Which items are low in stock?");
        AiQueryResponse response = aiAgentService.processQuery(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertNotNull(response.getRiskLevel());
        assertTrue(response.getAnswer().contains("SKU-001"));
        assertEquals("HIGH", response.getRiskLevel());
    }

    @Test
    void processQuery_shouldHandleTabularFallback() {
        InventoryItem item = new InventoryItem("SKU-001", 10, 5, "Warehouse-North");
        when(inventoryRepository.findAll()).thenReturn(List.of(item));

        AiQueryRequest request = new AiQueryRequest("can you share this data in more details in tabular form?");
        AiQueryResponse response = aiAgentService.processQuery(request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("| SKU | Category | Warehouse |"));
        assertTrue(response.getAnswer().contains("SKU-001"));
    }

    @Test
    void processQuery_shouldHandleSkuCountFallback() {
        InventoryItem item = new InventoryItem("SKU-001", 10, 5, "Warehouse-North");
        when(inventoryRepository.findAll()).thenReturn(List.of(item));

        AiQueryRequest request = new AiQueryRequest("how many different skus stock has?");
        AiQueryResponse response = aiAgentService.processQuery(request);

        assertNotNull(response);
        assertTrue(response.getAnswer().contains("1 distinct SKUs"));
    }
}
