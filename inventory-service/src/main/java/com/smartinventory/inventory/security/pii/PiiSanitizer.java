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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;

/**
 * Sanitization and masking engine for redacting Personally Identifiable Information (PII)
 * and confidential secrets across application logs, DTOs, and API responses.
 */
public final class PiiSanitizer {

    private PiiSanitizer() {}

    /**
     * Sanitizes free-form text by masking all detected PII entities with PARTIAL masking.
     */
    public static String sanitize(String text) {
        return sanitize(text, MaskStrategy.PARTIAL);
    }

    /**
     * Sanitizes free-form text using the specified mask strategy.
     */
    public static String sanitize(String text, MaskStrategy strategy) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String result = text;

        // 1. Mask Secrets first to avoid partial token matches
        Matcher secretMatcher = PiiDetector.SECRET_PATTERN.matcher(result);
        result = secretMatcher.replaceAll("[REDACTED_SECRET]");

        // 2. Mask SSN
        Matcher ssnMatcher = PiiDetector.SSN_PATTERN.matcher(result);
        StringBuilder ssnBuffer = new StringBuilder();
        while (ssnMatcher.find()) {
            String masked = maskSsn(ssnMatcher.group(), strategy);
            ssnMatcher.appendReplacement(ssnBuffer, Matcher.quoteReplacement(masked));
        }
        ssnMatcher.appendTail(ssnBuffer);
        result = ssnBuffer.toString();

        // 3. Mask Credit Cards (validating via Luhn algorithm)
        Matcher ccMatcher = PiiDetector.CREDIT_CARD_PATTERN.matcher(result);
        StringBuilder ccBuffer = new StringBuilder();
        while (ccMatcher.find()) {
            String match = ccMatcher.group();
            String digitsOnly = match.replaceAll("[-\\s]", "");
            if (digitsOnly.length() >= 13 && digitsOnly.length() <= 19 && PiiDetector.isValidLuhn(digitsOnly)) {
                String masked = maskCreditCard(match, strategy);
                ccMatcher.appendReplacement(ccBuffer, Matcher.quoteReplacement(masked));
            } else {
                ccMatcher.appendReplacement(ccBuffer, Matcher.quoteReplacement(match));
            }
        }
        ccMatcher.appendTail(ccBuffer);
        result = ccBuffer.toString();

        // 4. Mask Emails
        Matcher emailMatcher = PiiDetector.EMAIL_PATTERN.matcher(result);
        StringBuilder emailBuffer = new StringBuilder();
        while (emailMatcher.find()) {
            String masked = maskEmail(emailMatcher.group(), strategy);
            emailMatcher.appendReplacement(emailBuffer, Matcher.quoteReplacement(masked));
        }
        emailMatcher.appendTail(emailBuffer);
        result = emailBuffer.toString();

        // 5. Mask Phone Numbers
        Matcher phoneMatcher = PiiDetector.PHONE_PATTERN.matcher(result);
        StringBuilder phoneBuffer = new StringBuilder();
        while (phoneMatcher.find()) {
            String phoneCandidate = phoneMatcher.group();
            String digits = phoneCandidate.replaceAll("\\D", "");
            // Only mask candidates with 10-15 digits to avoid small numbers
            if (digits.length() >= 10 && digits.length() <= 15) {
                String masked = maskPhone(phoneCandidate, strategy);
                phoneMatcher.appendReplacement(phoneBuffer, Matcher.quoteReplacement(masked));
            } else {
                phoneMatcher.appendReplacement(phoneBuffer, Matcher.quoteReplacement(phoneCandidate));
            }
        }
        phoneMatcher.appendTail(phoneBuffer);
        result = phoneBuffer.toString();

        return result;
    }

    public static String maskEmail(String email, MaskStrategy strategy) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        if (strategy == MaskStrategy.FULL_REDACT) {
            return "[EMAIL_REDACTED]";
        }
        if (strategy == MaskStrategy.HASH) {
            return hash(email);
        }

        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];

        String maskedName;
        if (name.length() <= 2) {
            maskedName = name.charAt(0) + "*";
        } else {
            maskedName = name.charAt(0) + "***" + name.charAt(name.length() - 1);
        }

        return maskedName + "@" + domain;
    }

    public static String maskCreditCard(String card, MaskStrategy strategy) {
        if (card == null) {
            return "";
        }
        if (strategy == MaskStrategy.FULL_REDACT) {
            return "[CARD_REDACTED]";
        }
        if (strategy == MaskStrategy.HASH) {
            return hash(card);
        }

        String clean = card.replaceAll("[-\\s]", "");
        if (clean.length() < 4) {
            return "****";
        }
        String lastFour = clean.substring(clean.length() - 4);
        return "****-****-****-" + lastFour;
    }

    public static String maskSsn(String ssn, MaskStrategy strategy) {
        if (ssn == null) {
            return "";
        }
        if (strategy == MaskStrategy.FULL_REDACT) {
            return "[SSN_REDACTED]";
        }
        if (strategy == MaskStrategy.HASH) {
            return hash(ssn);
        }

        String digits = ssn.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***-**-****";
        }
        return "***-**-" + digits.substring(digits.length() - 4);
    }

    public static String maskPhone(String phone, MaskStrategy strategy) {
        if (phone == null) {
            return "";
        }
        if (strategy == MaskStrategy.FULL_REDACT) {
            return "[PHONE_REDACTED]";
        }
        if (strategy == MaskStrategy.HASH) {
            return hash(phone);
        }

        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***-***-****";
        }
        return "***-***-" + digits.substring(digits.length() - 4);
    }

    public static String maskIp(String ip, MaskStrategy strategy) {
        if (ip == null) {
            return "";
        }
        if (strategy == MaskStrategy.FULL_REDACT) {
            return "[IP_REDACTED]";
        }
        if (strategy == MaskStrategy.HASH) {
            return hash(ip);
        }

        String[] octets = ip.split("\\.");
        if (octets.length == 4) {
            return octets[0] + "." + octets[1] + ".***.***";
        }
        return "[IP_MASKED]";
    }

    public static String maskSecret(String secret) {
        if (secret == null) {
            return null;
        }
        return "[REDACTED_SECRET]";
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "hash:" + hexString.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            return "[HASH_UNAVAILABLE]";
        }
    }
}
