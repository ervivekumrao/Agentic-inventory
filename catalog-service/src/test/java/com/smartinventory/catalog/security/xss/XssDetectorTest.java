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

package com.smartinventory.catalog.security.xss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class XssDetectorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "SKU-001",
            "Laptop Pro 15",
            "High performance laptop with 16GB RAM and 512GB SSD",
            "Home & Kitchen",
            "Tools & Hardware",
            "70% Dark Chocolate Bar",
            "100% Cotton T-Shirt",
            "Price: $1,299.99"
    })
    void containsXss_shouldReturnFalse_forSafeInput(String safeInput) {
        assertFalse(XssDetector.containsXss(safeInput));
        assertDoesNotThrow(() -> XssDetector.validateInput(safeInput, "field"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "<SCRIPT SRC=\"http://evil.com/xss.js\"></SCRIPT>",
            "</script>",
            "<img src=\"x\" onerror=\"alert(1)\">",
            "<svg onload=alert(1)>",
            "<iframe src=\"http://attacker.com\">",
            "javascript:alert(document.cookie)",
            "vbscript:msgbox(\"XSS\")",
            "<body onload=alert('XSS')>",
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "<a href=\"#\" onclick=\"alert(1)\">Link</a>",
            "eval(alert(1))",
            "document.cookie"
    })
    void containsXss_shouldReturnTrue_forMaliciousInput(String maliciousInput) {
        assertTrue(XssDetector.containsXss(maliciousInput));
        XssException exception = assertThrows(XssException.class,
                () -> XssDetector.validateInput(maliciousInput, "testField"));
        assertEquals("testField", exception.getFieldName());
        assertNotNull(exception.getDetectedPattern());
    }

    @Test
    void containsXss_shouldReturnFalse_forNullOrBlankInput() {
        assertFalse(XssDetector.containsXss(null));
        assertFalse(XssDetector.containsXss(""));
        assertFalse(XssDetector.containsXss("   "));
        assertDoesNotThrow(() -> XssDetector.validateInput(null, "field"));
        assertDoesNotThrow(() -> XssDetector.validateInput("", "field"));
    }
}
