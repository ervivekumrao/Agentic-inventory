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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NoXssValidatorTest {

    private Validator validator;

    static class SampleDto {
        @NoXss
        private final String input;

        public SampleDto(String input) {
            this.input = input;
        }

        public String getInput() {
            return input;
        }
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testValidInputPassesValidation() {
        SampleDto dto = new SampleDto("SKU-VALID-123");
        assertEquals("SKU-VALID-123", dto.getInput());
        Set<ConstraintViolation<SampleDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testMaliciousInputFailsValidation() {
        SampleDto dto = new SampleDto("<script>alert('XSS')</script>");
        assertEquals("<script>alert('XSS')</script>", dto.getInput());
        Set<ConstraintViolation<SampleDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}
