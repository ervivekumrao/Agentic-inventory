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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NoPiiValidatorTest {

    private Validator validator;

    static class ProductInputDto {
        @NoPii
        private final String description;

        public ProductInputDto(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void testNonPiiInputPasses() {
        ProductInputDto dto = new ProductInputDto("Standard 15-inch aluminum laptop chassis");
        assertEquals("Standard 15-inch aluminum laptop chassis", dto.getDescription());
        Set<ConstraintViolation<ProductInputDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testPiiInputFailsValidation() {
        ProductInputDto dto = new ProductInputDto("Contact supplier at john@vendor.com");
        assertEquals("Contact supplier at john@vendor.com", dto.getDescription());
        Set<ConstraintViolation<ProductInputDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }
}
