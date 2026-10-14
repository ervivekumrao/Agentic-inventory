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

package com.smartinventory.inventory.security.sqli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlInjectionDetectorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "' OR '1'='1",
            "' or 1=1",
            "admin' --",
            "1; DROP TABLE inventory_items",
            "'; DELETE FROM users; --",
            "UNION SELECT * FROM users",
            "union all select null, null",
            "1' WAITFOR DELAY '0:0:5'--",
            "1 AND (SELECT 1 FROM (SELECT SLEEP(5))a)",
            "'; EXEC xp_cmdshell('dir'); --",
            "' OR true--",
            "admin' /* comment */",
            "1; TRUNCATE TABLE users"
    })
    void testDetectsKnownSqlInjectionPatterns(String maliciousInput) {
        assertTrue(SqlInjectionDetector.containsSqlInjection(maliciousInput),
                "Should detect SQL injection in: " + maliciousInput);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "LAPTOP-PRO-15",
            "Warehouse North",
            "Electronics & Accessories",
            "Normal text without any injection",
            "SKU_12345",
            "100",
            ""
    })
    void testPassesSafeInputs(String safeInput) {
        assertFalse(SqlInjectionDetector.containsSqlInjection(safeInput),
                "Should not flag safe input: " + safeInput);
    }

    @Test
    void testValidateInputThrowsOnInjection() {
        assertThrows(SqlInjectionException.class, () ->
                SqlInjectionDetector.validateInput("' OR '1'='1", "sku"));
    }

    @Test
    void testValidateInputDoesNotThrowOnSafeInput() {
        assertDoesNotThrow(() ->
                SqlInjectionDetector.validateInput("LAPTOP-PRO-15", "sku"));
    }

    @Test
    void testValidateQueryAllowsSafeSelect() {
        assertDoesNotThrow(() ->
                SqlInjectionDetector.validateQuery("SELECT * FROM inventory_items WHERE quantity > ?"));
    }

    @Test
    void testValidateQueryRejectsStackedDestructiveStatements() {
        assertThrows(SqlInjectionException.class, () ->
                SqlInjectionDetector.validateQuery("SELECT * FROM items; DROP TABLE items"));
    }

    @Test
    void testValidateQueryRejectsBlank() {
        assertThrows(SqlInjectionException.class, () ->
                SqlInjectionDetector.validateQuery("   "));
    }
}
