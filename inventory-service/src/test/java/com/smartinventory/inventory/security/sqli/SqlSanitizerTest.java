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

class SqlSanitizerTest {

    @Test
    void testSanitizeIdentifierValid() {
        assertEquals("sku", SqlSanitizer.sanitizeIdentifier("sku"));
        assertEquals("warehouse_location", SqlSanitizer.sanitizeIdentifier("warehouse_location"));
        assertEquals("inventory_items", SqlSanitizer.sanitizeIdentifier("inventory_items"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sku; DROP TABLE users",
            "quantity--",
            "table name with space",
            "DROP",
            "DELETE",
            "UNION",
            "",
            "   "
    })
    void testSanitizeIdentifierInvalid(String invalid) {
        assertThrows(SqlInjectionException.class, () ->
                SqlSanitizer.sanitizeIdentifier(invalid));
    }

    @Test
    void testSanitizeSortDirection() {
        assertEquals("ASC", SqlSanitizer.sanitizeSortDirection("asc"));
        assertEquals("ASC", SqlSanitizer.sanitizeSortDirection("ASC"));
        assertEquals("ASC", SqlSanitizer.sanitizeSortDirection(null));
        assertEquals("DESC", SqlSanitizer.sanitizeSortDirection("desc"));
        assertEquals("DESC", SqlSanitizer.sanitizeSortDirection("DESC"));

        assertThrows(SqlInjectionException.class, () ->
                SqlSanitizer.sanitizeSortDirection("INVALID"));
        assertThrows(SqlInjectionException.class, () ->
                SqlSanitizer.sanitizeSortDirection("; DROP TABLE items"));
    }

    @Test
    void testValidateLimit() {
        assertEquals(50, SqlSanitizer.validateLimit(50, 100));
        assertEquals(100, SqlSanitizer.validateLimit(500, 100)); // clamped

        assertThrows(SqlInjectionException.class, () -> SqlSanitizer.validateLimit(0, 100));
        assertThrows(SqlInjectionException.class, () -> SqlSanitizer.validateLimit(-5, 100));
    }

    @Test
    void testValidateOffset() {
        assertEquals(0, SqlSanitizer.validateOffset(0));
        assertEquals(20, SqlSanitizer.validateOffset(20));

        assertThrows(SqlInjectionException.class, () -> SqlSanitizer.validateOffset(-1));
    }

    @Test
    void testEscapeLiteral() {
        assertEquals("O''Reilly", SqlSanitizer.escapeLiteral("O'Reilly"));
        assertEquals("", SqlSanitizer.escapeLiteral(null));
        assertEquals("safe", SqlSanitizer.escapeLiteral("safe"));
    }
}
