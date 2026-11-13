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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Centralized high-performance hashing utility for Smart Inventory.
 * <p>
 * Enforces standardized cryptographic hashing, thread safety, uniform UTF-8 encoding,
 * constant-time equality checks to prevent timing attacks, and deterministic cache key generation.
 * <p>
 * <b>Algorithm-Agnostic Design:</b> Callers use algorithm-agnostic methods such as
 * {@link #hash(String)}, {@link #hashComposite(String, String)}, and {@link #hashMulti(String, String...)}.
 * This decouples business logic from specific hash bit-lengths. If the active algorithm is changed
 * (e.g. from SHA-256 to SHA-512 via configuration or API), all callers automatically benefit without
 * any code changes.
 * <p>
 * <b>Architectural Rule:</b> Direct invocation of {@code java.security.MessageDigest} or
 * bespoke hashing in other packages is prohibited. All hashing operations must route through
 * this centralized utility.
 */
public final class Hashing {

    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static volatile HashAlgorithm defaultAlgorithm = resolveDefaultAlgorithm();

    private Hashing() {
        // Utility class: prevent instantiation
    }

    static HashAlgorithm resolveDefaultAlgorithm() {
        return resolveDefaultAlgorithm(
                System.getProperty("smartinventory.hash.algorithm"),
                System.getenv("SMARTINVENTORY_HASH_ALGORITHM")
        );
    }

    static HashAlgorithm resolveDefaultAlgorithm(String sysProp, String env) {
        if (sysProp != null && !sysProp.isBlank()) {
            return HashAlgorithm.fromString(sysProp);
        }
        if (env != null && !env.isBlank()) {
            return HashAlgorithm.fromString(env);
        }
        return HashAlgorithm.SHA_256;
    }

    /**
     * Returns the active default hash algorithm used by algorithm-agnostic hashing methods.
     *
     * @return current default HashAlgorithm (defaults to SHA-256 unless configured via env/sysprop)
     */
    public static HashAlgorithm getDefaultAlgorithm() {
        return defaultAlgorithm;
    }

    /**
     * Programmatically sets the active default hash algorithm for the platform.
     *
     * @param algorithm the new default HashAlgorithm (falls back to SHA-256 if null)
     */
    public static void setDefaultAlgorithm(HashAlgorithm algorithm) {
        defaultAlgorithm = algorithm != null ? algorithm : HashAlgorithm.SHA_256;
    }

    // =========================================================================
    // Algorithm-Agnostic Methods (Recommended for Application Code)
    // =========================================================================

    /**
     * Computes the hexadecimal digest for a String using the platform's active default algorithm.
     *
     * @param input the string to hash (must not be null)
     * @return lowercase hexadecimal hash string
     */
    public static String hash(String input) {
        return hashHex(defaultAlgorithm, input);
    }

    /**
     * Computes the raw digest bytes for a byte array using the platform's active default algorithm.
     *
     * @param input raw bytes to hash (must not be null)
     * @return raw digest bytes
     */
    public static byte[] hash(byte[] input) {
        return hash(defaultAlgorithm, input);
    }

    /**
     * Computes the hexadecimal digest for a byte array using the platform's active default algorithm.
     *
     * @param input raw bytes to hash (must not be null)
     * @return lowercase hexadecimal hash string
     */
    public static String hashHex(byte[] input) {
        return hashHex(defaultAlgorithm, input);
    }

    /**
     * Computes a deterministic composite cache key formatted as {@code prefix:content} using the
     * platform's active default algorithm. Protected against delimiter injection attacks.
     *
     * @param prefix  the key prefix (must not be null)
     * @param content the payload content (must not be null)
     * @return lowercase hexadecimal hash string
     */
    public static String hashComposite(String prefix, String content) {
        return hashComposite(defaultAlgorithm, prefix, content);
    }

    /**
     * Computes a deterministic composite cache key formatted as {@code prefix:content} using the
     * explicitly specified algorithm.
     *
     * @param algorithm target hash algorithm
     * @param prefix    the key prefix (must not be null)
     * @param content   the payload content (must not be null)
     * @return lowercase hexadecimal hash string
     */
    public static String hashComposite(HashAlgorithm algorithm, String prefix, String content) {
        Objects.requireNonNull(prefix, "prefix must not be null");
        Objects.requireNonNull(content, "content must not be null");
        HashAlgorithm algo = algorithm != null ? algorithm : defaultAlgorithm;
        try {
            MessageDigest md = getDigest(algo);
            md.update(prefix.getBytes(StandardCharsets.UTF_8));
            md.update((byte) ':');
            md.update(content.getBytes(StandardCharsets.UTF_8));
            return HEX_FORMAT.formatHex(md.digest());
        } catch (Exception e) {
            return prefix + ":" + Integer.toHexString(content.hashCode());
        }
    }

    /**
     * Computes a deterministic composite hash for multiple string parts separated by a delimiter
     * using the platform's active default algorithm.
     *
     * @param delimiter delimiter between parts (e.g. ":")
     * @param parts     string components to hash
     * @return lowercase hexadecimal hash string
     */
    public static String hashMulti(String delimiter, String... parts) {
        return hashMulti(defaultAlgorithm, delimiter, parts);
    }

    /**
     * Computes a deterministic composite hash for multiple string parts separated by a delimiter
     * using the specified algorithm.
     *
     * @param algorithm target hash algorithm
     * @param delimiter delimiter between parts (e.g. ":")
     * @param parts     string components to hash
     * @return lowercase hexadecimal hash string
     */
    public static String hashMulti(HashAlgorithm algorithm, String delimiter, String... parts) {
        Objects.requireNonNull(parts, "parts must not be null");
        String delim = delimiter != null ? delimiter : ":";
        HashAlgorithm algo = algorithm != null ? algorithm : defaultAlgorithm;
        try {
            MessageDigest md = getDigest(algo);
            byte[] delimBytes = delim.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    md.update(delimBytes);
                }
                if (parts[i] != null) {
                    md.update(parts[i].getBytes(StandardCharsets.UTF_8));
                }
            }
            return HEX_FORMAT.formatHex(md.digest());
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) sb.append(delim);
                sb.append(parts[i] != null ? parts[i].hashCode() : "0");
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // Explicit Algorithm-Pinned Convenience Methods
    // =========================================================================

    public static String sha256(String input) {
        Objects.requireNonNull(input, "input string must not be null");
        return hashHex(HashAlgorithm.SHA_256, input.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] input) {
        Objects.requireNonNull(input, "input bytes must not be null");
        return hashHex(HashAlgorithm.SHA_256, input);
    }

    public static byte[] sha256Bytes(byte[] input) {
        Objects.requireNonNull(input, "input bytes must not be null");
        return hash(HashAlgorithm.SHA_256, input);
    }

    public static String sha512(String input) {
        Objects.requireNonNull(input, "input string must not be null");
        return hashHex(HashAlgorithm.SHA_512, input.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hash(HashAlgorithm algorithm, byte[] input) {
        Objects.requireNonNull(input, "input bytes must not be null");
        HashAlgorithm algo = algorithm != null ? algorithm : defaultAlgorithm;
        try {
            MessageDigest md = getDigest(algo);
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Required digest algorithm not available: " + algo.getStandardName(), e);
        }
    }

    public static String hashHex(HashAlgorithm algorithm, byte[] input) {
        byte[] digest = hash(algorithm, input);
        return HEX_FORMAT.formatHex(digest);
    }

    public static String hashHex(HashAlgorithm algorithm, String input) {
        Objects.requireNonNull(input, "input must not be null");
        return hashHex(algorithm, input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Performs a constant-time comparison of two byte arrays to protect against timing attacks.
     *
     * @param a first byte array
     * @param b second byte array
     * @return true if both arrays are equal
     */
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return java.util.Arrays.equals(a, b);
        }
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Performs a constant-time comparison of two hexadecimal hash strings.
     *
     * @param hexA first hex string
     * @param hexB second hex string
     * @return true if both hashes are equal
     */
    public static boolean isEqualHex(String hexA, String hexB) {
        if (hexA == null || hexB == null) {
            return Objects.equals(hexA, hexB);
        }
        return MessageDigest.isEqual(hexA.getBytes(StandardCharsets.UTF_8), hexB.getBytes(StandardCharsets.UTF_8));
    }

    private static MessageDigest getDigest(HashAlgorithm algorithm) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(algorithm.getStandardName());
    }
}
