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

package com.smartinventory.catalog.exception;

import com.smartinventory.catalog.security.xss.XssException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleXssException_shouldReturnBadRequest() {
        XssException ex = new XssException("Potential XSS detected", "sku", "<script>");
        ResponseEntity<Map<String, Object>> response = handler.handleXssException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Security violation: Potential Cross-Site Scripting (XSS) detected.", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input argument");
        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid input argument", response.getBody().get("error"));
        assertEquals(400, response.getBody().get("status"));
    }
}
