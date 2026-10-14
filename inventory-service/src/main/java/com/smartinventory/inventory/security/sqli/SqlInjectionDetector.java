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

import java.util.List;
import java.util.regex.Pattern;

/**
 * High-performance detection engine for identifying SQL injection signatures and attack vectors.
 */
public final class SqlInjectionDetector {

    private static final Logger log = LoggerFactory.getLogger(SqlInjectionDetector.class);

    private static final List<Pattern> SQLI_PATTERNS = List.of(
            // Tautology / Boolean-based blind patterns (e.g. ' OR '1'='1', ' OR 1=1, OR 'a'='a')
            Pattern.compile("(?i)(?:'|\\b)\\s*(?:or|and)\\s+['\"]?\\w+['\"]?\\s*=\\s*['\"]?\\w+['\"]?"),
            Pattern.compile("(?i)(?:'|\\b)\\s*(?:or|and)\\s+\\d+\\s*=\\s*\\d+"),
            Pattern.compile("(?i)(?:'|\\b)\\s*(?:or|and)\\s+true\\b"),

            // Stacked queries: semicolon followed by DDL or DML statements
            Pattern.compile("(?i);\\s*(?:drop|delete|update|insert|alter|truncate|exec|execute|create|grant|revoke|shutdown)\\b"),

            // Union-based injection
            Pattern.compile("(?i)\\bunion(?:\\s+all)?\\s+select\\b"),

            // Inline comment injections used to truncate queries (-- or /* ... */ or #)
            Pattern.compile("(?i)(?:--|/\\*.*?\\*/|#)"),

            // Time-based blind injection vectors
            Pattern.compile("(?i)\\b(?:waitfor\\s+delay|sleep\\s*\\(|pg_sleep\\s*\\(|benchmark\\s*\\(|dbms_lock\\.sleep)\\b"),

            // Command execution and stored procedures
            Pattern.compile("(?i)\\b(?:xp_cmdshell|exec(?:ute)?\\s+(?:master\\.)?xp_|sp_executesql)\\b"),

            // Error-based / File I/O extraction functions
            Pattern.compile("(?i)\\b(?:extractvalue\\s*\\(|updatexml\\s*\\(|load_file\\s*\\(|into\\s+outfile|into\\s+dumpfile)\\b"),

            // Quoted string escape with comment or tautology (e.g., admin'--)
            Pattern.compile("(?i)'\\s*(?:--|#|/\\*)")
    );

    // Queries should only allow expected single-statement structure when validated as entire SQL commands
    private static final Pattern DANGEROUS_MULTI_STATEMENT = Pattern.compile("(?i);\\s*(?:drop|delete|alter|truncate|exec|shutdown)\\b");

    private SqlInjectionDetector() {}

    /**
     * Checks if the untrusted input contains any SQL injection signatures.
     *
     * @param input raw untrusted string
     * @return true if an injection vector was detected, false otherwise
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String normalized = input.trim();
        for (Pattern pattern : SQLI_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts that an input string does not contain SQL injection vectors.
     * Throws {@link SqlInjectionException} if an attack is detected.
     *
     * @param input untrusted parameter input
     * @param fieldName logical field name for logging and auditing
     */
    public static void validateInput(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            return;
        }

        String normalized = input.trim();
        for (Pattern pattern : SQLI_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                log.warn("SQL injection attempt detected in field '{}': matched rule '{}'", fieldName, pattern.pattern());
                throw new SqlInjectionException(
                        "Input validation failure: Potential SQL injection detected in field '" + fieldName + "'",
                        fieldName,
                        pattern.pattern()
                );
            }
        }
    }

    /**
     * Validates a complete SQL statement before execution to ensure stacked destructive commands
     * or unauthorized multi-statements are prevented.
     *
     * @param sql query string to validate
     */
    public static void validateQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new SqlInjectionException("SQL query string cannot be null or blank");
        }

        if (DANGEROUS_MULTI_STATEMENT.matcher(sql).find()) {
            log.error("Dangerous multi-statement SQL query execution attempt blocked: {}", sql);
            throw new SqlInjectionException("Dangerous stacked SQL statement rejected: Unauthorized command sequence");
        }
    }
}
