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

package com.smartinventory.inventory.security.hash;

/**
 * Supported hashing algorithms in Smart Inventory.
 * Standardized across the entire platform.
 */
public enum HashAlgorithm {

    /**
     * SHA-256 (256-bit secure hash). Default standard for PII masking & cache keys.
     */
    SHA_256("SHA-256", 64),

    /**
     * SHA-512 (512-bit secure hash) for high-entropy requirements.
     */
    SHA_512("SHA-512", 128),

    /**
     * SHA-384 (384-bit secure hash).
     */
    SHA_384("SHA-384", 96),

    /**
     * Legacy MD5 hash. Marked as insecure; provided strictly for legacy compatibility.
     * @deprecated Use {@link #SHA_256} instead.
     */
    @Deprecated
    MD5("MD5", 32);

    private final String standardName;
    private final int hexLength;

    HashAlgorithm(String standardName, int hexLength) {
        this.standardName = standardName;
        this.hexLength = hexLength;
    }

    public String getStandardName() {
        return standardName;
    }

    public int getHexLength() {
        return hexLength;
    }

    /**
     * Parses a string representation of an algorithm (e.g. "SHA-256", "SHA_512", "sha512").
     *
     * @param name algorithm name
     * @return matching HashAlgorithm or SHA_256 as fallback
     */
    public static HashAlgorithm fromString(String name) {
        if (name == null || name.isBlank()) {
            return SHA_256;
        }
        String normalized = name.trim().toUpperCase().replace("-", "_");
        for (HashAlgorithm algo : values()) {
            if (algo.name().equals(normalized)) {
                return algo;
            }
        }
        return SHA_256;
    }
}
