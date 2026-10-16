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

/**
 * Security exception thrown when potential Cross-Site Scripting (XSS) attacks or unsafe HTML constructs are detected.
 */
@SuppressWarnings("unused")
public class XssException extends RuntimeException {

    private final String fieldName;
    private final String detectedPattern;

    public XssException(String message) {
        super(message);
        this.fieldName = null;
        this.detectedPattern = null;
    }

    public XssException(String message, String fieldName, String detectedPattern) {
        super(message);
        this.fieldName = fieldName;
        this.detectedPattern = detectedPattern;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getDetectedPattern() {
        return detectedPattern;
    }
}
