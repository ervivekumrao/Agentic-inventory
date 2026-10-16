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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * High-performance detection engine for identifying Cross-Site Scripting (XSS) signatures and attack vectors.
 */
public final class XssDetector {

    private static final Logger log = LoggerFactory.getLogger(XssDetector.class);

    private static final List<Pattern> XSS_PATTERNS = List.of(
            // Script tags: <script ...> or </script>
            Pattern.compile("(?i)<\\s*script\\b[^>]*>"),
            Pattern.compile("(?i)</\\s*script\\s*>"),

            // Javascript / VBScript / livescript pseudo-protocols
            Pattern.compile("(?i)(?:javascript|vbscript|livescript)\\s*:"),

            // Inline event handlers: onload=, onerror=, onclick=, etc.
            Pattern.compile("(?i)\\bon[a-zA-Z]+\\s*="),

            // Dangerous HTML tags commonly exploited for injection
            Pattern.compile("(?i)<\\s*(?:iframe|object|embed|applet|svg|meta|link|frameset|base|body|form|style)\\b"),

            // Evaluation & script execution expressions
            Pattern.compile("(?i)\\b(?:eval\\s*\\(|expression\\s*\\(|document\\.(?:cookie|location|write))"),

            // Data URI with text/html
            Pattern.compile("(?i)data\\s*:\\s*text/html"),

            // Image tag with inline event or script source
            Pattern.compile("(?i)<\\s*img\\b[^>]*\\bon[a-zA-Z]+\\s*=")
    );

    private XssDetector() {}

    /**
     * Checks if untrusted input contains any XSS attack signatures.
     *
     * @param input raw untrusted string
     * @return true if an XSS signature was detected, false otherwise
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        String normalized = input.trim();
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts that an input string does not contain XSS attack vectors.
     * Throws {@link XssException} if an attack is detected.
     *
     * @param input untrusted parameter input
     * @param fieldName logical field name for logging and auditing
     */
    public static void validateInput(String input, String fieldName) {
        if (input == null || input.isBlank()) {
            return;
        }

        String normalized = input.trim();
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                log.warn("XSS attack attempt detected in field '{}': matched rule '{}'", fieldName, pattern.pattern());
                throw new XssException(
                        "Input validation failure: Potential XSS script detected in field '" + fieldName + "'",
                        fieldName,
                        pattern.pattern()
                );
            }
        }
    }
}
