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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiSanitizerTest {

    @Test
    void testMaskEmail() {
        assertEquals("j***e@example.com", PiiSanitizer.maskEmail("john.doe@example.com", MaskStrategy.PARTIAL));
        assertEquals("[EMAIL_REDACTED]", PiiSanitizer.maskEmail("john.doe@example.com", MaskStrategy.FULL_REDACT));
        assertTrue(PiiSanitizer.maskEmail("john.doe@example.com", MaskStrategy.HASH).startsWith("hash:"));
    }

    @Test
    void testMaskCreditCard() {
        assertEquals("****-****-****-1111", PiiSanitizer.maskCreditCard("4111-1111-1111-1111", MaskStrategy.PARTIAL));
        assertEquals("[CARD_REDACTED]", PiiSanitizer.maskCreditCard("4111-1111-1111-1111", MaskStrategy.FULL_REDACT));
    }

    @Test
    void testMaskSsn() {
        assertEquals("***-**-6789", PiiSanitizer.maskSsn("123-45-6789", MaskStrategy.PARTIAL));
        assertEquals("[SSN_REDACTED]", PiiSanitizer.maskSsn("123-45-6789", MaskStrategy.FULL_REDACT));
    }

    @Test
    void testMaskPhone() {
        assertEquals("***-***-4567", PiiSanitizer.maskPhone("+1 555-123-4567", MaskStrategy.PARTIAL));
        assertEquals("[PHONE_REDACTED]", PiiSanitizer.maskPhone("555-123-4567", MaskStrategy.FULL_REDACT));
    }

    @Test
    void testMaskIp() {
        assertEquals("192.168.***.***", PiiSanitizer.maskIp("192.168.1.100", MaskStrategy.PARTIAL));
        assertEquals("[IP_REDACTED]", PiiSanitizer.maskIp("192.168.1.100", MaskStrategy.FULL_REDACT));
    }

    @Test
    void testSanitizeMixedText() {
        String input = "Customer john.doe@example.com with phone 555-123-4567 paid via 4111-1111-1111-1111 and token: bearer secretToken";
        String sanitized = PiiSanitizer.sanitize(input);

        assertFalse(sanitized.contains("john.doe@example.com"));
        assertFalse(sanitized.contains("4111-1111-1111-1111"));
        assertFalse(sanitized.contains("secretToken"));
        assertTrue(sanitized.contains("j***e@example.com"));
        assertTrue(sanitized.contains("****-****-****-1111"));
    }
}
