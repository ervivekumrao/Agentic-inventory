package com.smartinventory.inventory.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid stock adjustment");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid stock adjustment", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void handleOptimisticLockingException_shouldReturnConflict() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(
                "InventoryItem", "SKU-001");
        ResponseEntity<Map<String, Object>> response = handler.handleOptimisticLockingException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().get("error").toString().contains("concurrently"));
        assertEquals(409, response.getBody().get("status"));
    }
}
