# ADR 0009: Centralized Algorithm-Agnostic Hashing Framework

## Status
Approved

## Context
Previously, hashing operations across microservices were fragmented or directly invoked `java.security.MessageDigest.getInstance("SHA-256")`. This tight coupling prevented upgrading hash algorithms (e.g., from SHA-256 to SHA-512) without code changes across multiple packages. Furthermore, raw string comparison of hash digests exposed the system to side-channel timing attacks.

## Decision
We implement a **Centralized Algorithm-Agnostic Hashing Framework** in `com.smartinventory.inventory.security.hash` (modeled after the `JitForge` architecture):

1. **Algorithm Enum (`HashAlgorithm`)**: Standardizes supported cryptographic hashing algorithms (`SHA-256`, `SHA-512`, `SHA-384`, and `MD5` deprecated for legacy compatibility).
2. **Algorithm-Agnostic Utility (`Hashing`)**: Centralized helper class providing `hash(String)`, `hash(byte[])`, `hashHex(String)`, `hashComposite(prefix, content)`, and `hashMulti(delimiter, parts...)`. Callers remain algorithm-agnostic.
3. **Environment & Runtime Configurable**: The default active algorithm can be set via environment variable (`SMARTINVENTORY_HASH_ALGORITHM`), system property (`smartinventory.hash.algorithm`), or dynamically at runtime via `/api/admin/config`.
4. **Timing Attack Protection**: Provides constant-time array and hex string equality checks (`isEqual` & `isEqualHex`) using `MessageDigest.isEqual`.
5. **PII & Subsystem Integration**: Replaces direct `MessageDigest` calls in `PiiSanitizer` (for `MaskStrategy.HASH`) and cache key generation with calls to `Hashing`.

## Consequences
### Positive
- **Algorithm Independence**: Upgrading system-wide hashing algorithms requires zero code changes at call-sites.
- **Timing Attack Defense**: Constant-time equality checks prevent side-channel timing attacks.
- **Centralized Security Audit**: All hashing operations route through a single, audited package.

### Negative
- **Deprecation Governance**: Strict policy required to prevent accidental usage of insecure legacy algorithms like MD5.
