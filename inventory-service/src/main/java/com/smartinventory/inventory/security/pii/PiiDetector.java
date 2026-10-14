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

package com.smartinventory.inventory.security.pii;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-precision detector for identifying Personally Identifiable Information (PII)
 * within free-form strings, request payloads, and log messages.
 */
public final class PiiDetector {

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}",
            Pattern.CASE_INSENSITIVE
    );

    public static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}"
    );

    public static final Pattern SSN_PATTERN = Pattern.compile(
            "(?<![A-Za-z0-9-])(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}(?![A-Za-z0-9-])"
    );

    public static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
            "\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11}|\\d{4}[-\\s]\\d{4}[-\\s]\\d{4}[-\\s]\\d{4})\\b"
    );

    public static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(?:bearer\\s+\\S+|(?:password|secret|token|api[_-]?key)\\s*[:=]\\s*(?:bearer\\s+)?\\S+|ghp_[A-Za-z0-9]+|eyJ[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+)\\b"
    );

    public static final Pattern IPV4_PATTERN = Pattern.compile(
            "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b"
    );

    private PiiDetector() {}

    /**
     * Checks if text contains any known category of PII.
     */
    public static boolean containsPii(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return isEmailPresent(text) ||
                isPhonePresent(text) ||
                isSsnPresent(text) ||
                isCreditCardPresent(text) ||
                isSecretPresent(text);
    }

    /**
     * Returns the set of PII types detected in the text.
     */
    public static Set<PiiType> detectPiiTypes(String text) {
        Set<PiiType> detected = EnumSet.noneOf(PiiType.class);
        if (text == null || text.isBlank()) {
            return detected;
        }

        if (isEmailPresent(text)) detected.add(PiiType.EMAIL);
        if (isPhonePresent(text)) detected.add(PiiType.PHONE);
        if (isSsnPresent(text)) detected.add(PiiType.SSN);
        if (isCreditCardPresent(text)) detected.add(PiiType.CREDIT_CARD);
        if (isSecretPresent(text)) detected.add(PiiType.SECRET);
        if (isIpPresent(text)) detected.add(PiiType.IP_ADDRESS);

        return detected;
    }

    public static boolean isEmailPresent(String text) {
        return text != null && EMAIL_PATTERN.matcher(text).find();
    }

    public static boolean isPhonePresent(String text) {
        return text != null && PHONE_PATTERN.matcher(text).find();
    }

    public static boolean isSsnPresent(String text) {
        return text != null && SSN_PATTERN.matcher(text).find();
    }

    public static boolean isSecretPresent(String text) {
        return text != null && SECRET_PATTERN.matcher(text).find();
    }

    public static boolean isIpPresent(String text) {
        return text != null && IPV4_PATTERN.matcher(text).find();
    }

    /**
     * Checks for credit card presence and validates using the Luhn checksum algorithm.
     */
    public static boolean isCreditCardPresent(String text) {
        if (text == null) {
            return false;
        }
        Matcher matcher = CREDIT_CARD_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[-\\s]", "");
            if (candidate.length() >= 13 && candidate.length() <= 19 && isValidLuhn(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies Luhn algorithm (Mod 10) checksum for card numbers.
     */
    public static boolean isValidLuhn(String number) {
        if (number == null || number.length() < 13 || number.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            char c = number.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            int n = c - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
}
