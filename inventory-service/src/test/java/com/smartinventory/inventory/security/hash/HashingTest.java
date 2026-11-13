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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HashingTest {

    @AfterEach
    void resetDefaultAlgorithm() {
        Hashing.setDefaultAlgorithm(HashAlgorithm.SHA_256);
    }

    @Test
    void testHashAlgorithmFromString() {
        assertEquals(HashAlgorithm.SHA_256, HashAlgorithm.fromString("SHA-256"));
        assertEquals(HashAlgorithm.SHA_512, HashAlgorithm.fromString("SHA_512"));
        assertEquals(HashAlgorithm.SHA_384, HashAlgorithm.fromString("sha-384"));
        assertEquals(HashAlgorithm.MD5, HashAlgorithm.fromString("md5"));
        assertEquals(HashAlgorithm.SHA_256, HashAlgorithm.fromString("UNKNOWN"));
        assertEquals(HashAlgorithm.SHA_256, HashAlgorithm.fromString(null));
    }

    @Test
    void testDefaultSha256Hashing() {
        String input = "Hello Smart Inventory";
        String hash = Hashing.hash(input);

        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertEquals(Hashing.sha256(input), hash);
    }

    @Test
    void testSha512Hashing() {
        String input = "Hello Smart Inventory";
        String hash = Hashing.hashHex(HashAlgorithm.SHA_512, input);

        assertNotNull(hash);
        assertEquals(128, hash.length());
        assertEquals(Hashing.sha512(input), hash);
    }

    @Test
    void testRuntimeAlgorithmSwitching() {
        String input = "Runtime Hash Test";
        Hashing.setDefaultAlgorithm(HashAlgorithm.SHA_512);

        String hash512 = Hashing.hash(input);
        assertEquals(128, hash512.length());

        Hashing.setDefaultAlgorithm(HashAlgorithm.SHA_256);
        String hash256 = Hashing.hash(input);
        assertEquals(64, hash256.length());
    }

    @Test
    void testHashComposite() {
        String composite = Hashing.hashComposite("CatalogCache", "PROD-100");
        assertNotNull(composite);
        assertEquals(64, composite.length());

        String explicit512 = Hashing.hashComposite(HashAlgorithm.SHA_512, "CatalogCache", "PROD-100");
        assertEquals(128, explicit512.length());
    }

    @Test
    void testHashMulti() {
        String multi = Hashing.hashMulti(":", "part1", "part2", "part3");
        assertNotNull(multi);
        assertEquals(64, multi.length());
    }

    @Test
    void testIsEqualHexConstantTime() {
        String input1 = "SecureToken123";
        String input2 = "SecureToken123";
        String input3 = "DifferentToken";

        String hash1 = Hashing.hash(input1);
        String hash2 = Hashing.hash(input2);
        String hash3 = Hashing.hash(input3);

        assertTrue(Hashing.isEqualHex(hash1, hash2));
        assertFalse(Hashing.isEqualHex(hash1, hash3));
        assertTrue(Hashing.isEqual(input1.getBytes(StandardCharsets.UTF_8), input2.getBytes(StandardCharsets.UTF_8)));
        assertFalse(Hashing.isEqual(input1.getBytes(StandardCharsets.UTF_8), input3.getBytes(StandardCharsets.UTF_8)));
    }
}
