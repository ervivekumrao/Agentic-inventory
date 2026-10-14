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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sanitization and validation utilities for database query identifiers, sorting parameters, and pagination.
 */
public final class SqlSanitizer {

    private static final Logger log = LoggerFactory.getLogger(SqlSanitizer.class);

    private static final Pattern SAFE_SQL_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,63}$");

    private static final Set<String> RESERVED_DISALLOWED_WORDS = Set.of(
            "DROP", "DELETE", "TRUNCATE", "ALTER", "EXEC", "EXECUTE", "SHUTDOWN",
            "UNION", "GRANT", "REVOKE", "INSERT", "UPDATE"
    );

    public static final int DEFAULT_MAX_LIMIT = 1000;

    private SqlSanitizer() {}

    /**
     * Sanitizes and verifies that an SQL identifier (column name, table name, alias)
     * conforms to safe naming standards and is not an injection vector.
     *
     * @param identifier the raw table or column name
     * @return the safe identifier
     * @throws SqlInjectionException if the identifier contains invalid characters
     */
    public static String sanitizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new SqlInjectionException("SQL identifier cannot be null or empty");
        }

        String trimmed = identifier.trim();
        if (!SAFE_SQL_IDENTIFIER.matcher(trimmed).matches()) {
            log.warn("Invalid SQL identifier rejected: '{}'", identifier);
            throw new SqlInjectionException("Invalid SQL identifier format: '" + identifier + "'");
        }

        if (RESERVED_DISALLOWED_WORDS.contains(trimmed.toUpperCase())) {
            log.warn("Disallowed reserved keyword used as SQL identifier: '{}'", identifier);
            throw new SqlInjectionException("Reserved keyword cannot be used as SQL identifier: '" + identifier + "'");
        }

        return trimmed;
    }

    /**
     * Sanitizes sort direction to strictly ASC or DESC.
     *
     * @param direction direction string (e.g., "asc", "DESC")
     * @return normalized "ASC" or "DESC"
     */
    public static String sanitizeSortDirection(String direction) {
        if (direction == null || direction.isBlank() || direction.equalsIgnoreCase("ASC")) {
            return "ASC";
        }
        if (direction.equalsIgnoreCase("DESC")) {
            return "DESC";
        }
        throw new SqlInjectionException("Invalid SQL sort direction: '" + direction + "'. Must be ASC or DESC.");
    }

    /**
     * Validates that pagination limit is positive and within safe bounds.
     *
     * @param limit requested limit
     * @param maxLimit maximum allowed limit
     * @return the validated limit
     */
    public static int validateLimit(int limit, int maxLimit) {
        if (limit <= 0) {
            throw new SqlInjectionException("Pagination limit must be greater than 0, received: " + limit);
        }
        if (limit > maxLimit) {
            log.debug("Pagination limit {} exceeded max {}, constraining to {}", limit, maxLimit, maxLimit);
            return maxLimit;
        }
        return limit;
    }

    /**
     * Validates that pagination limit is positive and within default max bound (1000).
     */
    public static int validateLimit(int limit) {
        return validateLimit(limit, DEFAULT_MAX_LIMIT);
    }

    /**
     * Validates that pagination offset is non-negative.
     */
    public static int validateOffset(int offset) {
        if (offset < 0) {
            throw new SqlInjectionException("Pagination offset cannot be negative, received: " + offset);
        }
        return offset;
    }

    /**
     * Escapes standard SQL single quotes in string literals.
     * NOTE: Parameterized queries (PreparedStatement) should always be used in preference to string concatenation.
     */
    public static String escapeLiteral(String literal) {
        if (literal == null) {
            return "";
        }
        return literal.replace("'", "''");
    }
}
