package com.smartinventory.integration.inventory;

import com.smartinventory.inventory.InventoryApplication;
import com.smartinventory.inventory.dto.AiQueryRequest;
import com.smartinventory.inventory.dto.AiQueryResponse;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = InventoryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiAgentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    @Test
    void testAiAgentChatWithHydratedContext() {
        InventoryItem item1 = new InventoryItem("SKU-AI-001", 3, 10, "Warehouse-North");
        InventoryItem item2 = new InventoryItem("SKU-AI-002", 50, 10, "Warehouse-South");
        inventoryRepository.save(item1);
        inventoryRepository.save(item2);

        AiQueryRequest request = new AiQueryRequest("Which items are low in stock?");

        ResponseEntity<AiQueryResponse> response = restTemplate
                .postForEntity("/api/ai/agent/chat", request, AiQueryResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getAnswer().contains("SKU-AI-001") || response.getBody().getAnswer().contains("low stock") || response.getBody().getAnswer().contains("Hello"));
        assertNotNull(response.getBody().getRiskLevel());
        assertFalse(response.getBody().getRecommendations().isEmpty());
    }
}
