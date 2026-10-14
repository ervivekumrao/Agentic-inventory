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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiMaskingSerializerTest {

    static class UserDto {
        @PiiData(type = PiiType.EMAIL, strategy = MaskStrategy.PARTIAL)
        private final String email;

        @PiiData(type = PiiType.PHONE, strategy = MaskStrategy.FULL_REDACT)
        private final String phone;

        public UserDto(String email, String phone) {
            this.email = email;
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public String getPhone() {
            return phone;
        }
    }

    @Test
    void testJacksonSerializesMaskedPii() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        UserDto dto = new UserDto("alice@example.com", "555-123-4567");

        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("555-123-4567", dto.getPhone());
        String json = mapper.writeValueAsString(dto);

        assertFalse(json.contains("alice@example.com"));
        assertTrue(json.contains("a***e@example.com"));
        assertTrue(json.contains("[PHONE_REDACTED]"));
    }
}
