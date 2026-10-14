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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PiiDetectorTest {

    @Test
    void testDetectEmail() {
        assertTrue(PiiDetector.isEmailPresent("Contact me at user@smartinventory.com"));
        assertTrue(PiiDetector.isEmailPresent("admin.ops@company.org"));
        assertFalse(PiiDetector.isEmailPresent("Not an email: user@"));
        assertFalse(PiiDetector.isEmailPresent("just plain text"));
    }

    @Test
    void testDetectPhone() {
        assertTrue(PiiDetector.isPhonePresent("Call customer support at +1 555-123-4567"));
        assertTrue(PiiDetector.isPhonePresent("(800) 555-0199"));
        assertTrue(PiiDetector.isPhonePresent("555-867-5309"));
        assertFalse(PiiDetector.isPhonePresent("SKU-123-45"));
    }

    @Test
    void testDetectSsn() {
        assertTrue(PiiDetector.isSsnPresent("SSN is 123-45-6789"));
        assertFalse(PiiDetector.isSsnPresent("000-12-3456")); // 000 prefix invalid SSN
        assertFalse(PiiDetector.isSsnPresent("SKU-123-45-6789-A"));
    }

    @Test
    void testDetectCreditCardWithLuhn() {
        // Valid Luhn test card (Visa test: 4532-0151-1283-0366 or 4111-1111-1111-1111)
        assertTrue(PiiDetector.isCreditCardPresent("Payment card: 4111-1111-1111-1111"));
        assertTrue(PiiDetector.isCreditCardPresent("4111 1111 1111 1111"));

        // Invalid Luhn should fail
        assertFalse(PiiDetector.isCreditCardPresent("1234-5678-9012-3456"));
    }

    @Test
    void testDetectSecret() {
        assertTrue(PiiDetector.isSecretPresent("Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.test.token"));
        assertTrue(PiiDetector.isSecretPresent("password: secretPassword123"));
        assertTrue(PiiDetector.isSecretPresent("api_key = key_998877"));
        assertFalse(PiiDetector.isSecretPresent("Normal inventory description"));
    }

    @Test
    void testDetectIp() {
        assertTrue(PiiDetector.isIpPresent("Client IP is 192.168.1.50"));
        assertTrue(PiiDetector.isIpPresent("10.0.0.1"));
        assertFalse(PiiDetector.isIpPresent("Version 3.4.7"));
    }

    @Test
    void testDetectPiiTypes() {
        String mixed = "User email user@test.com with phone 555-123-4567 and password: secret";
        Set<PiiType> types = PiiDetector.detectPiiTypes(mixed);

        assertTrue(types.contains(PiiType.EMAIL));
        assertTrue(types.contains(PiiType.PHONE));
        assertTrue(types.contains(PiiType.SECRET));
    }
}
