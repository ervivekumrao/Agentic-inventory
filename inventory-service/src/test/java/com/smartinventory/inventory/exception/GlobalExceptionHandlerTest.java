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

package com.smartinventory.inventory.exception;

import com.smartinventory.inventory.security.sqli.SqlInjectionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSqlInjectionException_shouldReturnBadRequestWithSecurityNotice() {
        SqlInjectionException ex = new SqlInjectionException("SQL injection detected", "sku", "' OR '1'='1");
        assertEquals("sku", ex.getFieldName());
        assertEquals("' OR '1'='1", ex.getDetectedPattern());
        ResponseEntity<Map<String, Object>> response = handler.handleSqlInjectionException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Security violation: Potential SQL injection detected.", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequestAndSanitizePii() {
        IllegalArgumentException ex = new IllegalArgumentException("User user@example.com is invalid");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        String errorMsg = (String) response.getBody().get("error");
        assertFalse(errorMsg.contains("user@example.com"));
        assertTrue(errorMsg.contains("u***r@example.com"));
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

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new RuntimeException("Unexpected error");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An internal server error occurred.", response.getBody().get("error"));
        assertEquals(500, response.getBody().get("status"));
    }
}
