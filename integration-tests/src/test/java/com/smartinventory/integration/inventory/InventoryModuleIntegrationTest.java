package com.smartinventory.integration.inventory;

import com.smartinventory.inventory.InventoryApplication;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = InventoryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryModuleIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
        // Seed test admin user via public registration / user management endpoint
        Map<String, Object> userPayload = Map.of(
                "username", "admin",
                "password", "password",
                "roles", List.of("ROLE_ADMIN")
        );
        restTemplate.postForEntity("/api/admin/users", userPayload, Map.class);
    }

    @Test
    void testInventoryModuleEndToEnd() {
        InventoryItem item = new InventoryItem("SKU-MOD-001", 150, 20, "Warehouse-B");

        ResponseEntity<InventoryItem> response = restTemplate
                .withBasicAuth("admin", "password")
                .postForEntity("/api/inventory", item, InventoryItem.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("SKU-MOD-001", response.getBody().getSku());
        assertEquals(150, response.getBody().getQuantity());
    }
}
