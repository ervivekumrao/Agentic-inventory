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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

/**
 * Jackson serializer that applies automated PII masking based on {@link PiiData} configuration.
 */
public class PiiMaskingSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final PiiType type;
    private final MaskStrategy strategy;

    @SuppressWarnings("unused")
    public PiiMaskingSerializer() {
        this(PiiType.GENERIC, MaskStrategy.PARTIAL);
    }

    public PiiMaskingSerializer(PiiType type, MaskStrategy strategy) {
        this.type = type;
        this.strategy = strategy;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String masked;
        switch (type) {
            case EMAIL -> masked = PiiSanitizer.maskEmail(value, strategy);
            case CREDIT_CARD -> masked = PiiSanitizer.maskCreditCard(value, strategy);
            case SSN -> masked = PiiSanitizer.maskSsn(value, strategy);
            case PHONE -> masked = PiiSanitizer.maskPhone(value, strategy);
            case IP_ADDRESS -> masked = PiiSanitizer.maskIp(value, strategy);
            case SECRET -> masked = PiiSanitizer.maskSecret(value);
            case GENERIC -> masked = PiiSanitizer.sanitize(value, strategy);
            default -> masked = PiiSanitizer.sanitize(value, strategy);
        }

        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        if (property != null) {
            PiiData piiData = property.getAnnotation(PiiData.class);
            if (piiData != null) {
                return new PiiMaskingSerializer(piiData.type(), piiData.strategy());
            }
        }
        return this;
    }
}
