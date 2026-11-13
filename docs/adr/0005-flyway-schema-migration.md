# ADR 0005: Deterministic Schema Migration with Flyway Across SQL Dialects

## Status
Approved

## Context
Relying on ORM auto-generation (`hibernate.ddl-auto=update`) for relational database schemas causes unversioned, non-repeatable DDL changes. Under production environments across multi-vendor databases (PostgreSQL, MySQL, Oracle, MSSQL, DB2), automatic DDL generation cannot handle column renames safely, lacks migration history tracking, and risks data loss.

## Decision
We integrated **Flyway Schema Migration** into `inventory-service`:

1. **Gradle Dependency**: Added `org.flywaydb:flyway-core` to `inventory-service/build.gradle.kts`.
2. **Versioned SQL Scripts**: Versioned migration scripts are stored under `src/main/resources/db/migration/` (e.g. `V1__init_schema.sql`).
3. **Configuration**: Set `spring.jpa.hibernate.ddl-auto=validate` in production profiles to ensure Hibernate validates the schema against Flyway migrations without altering tables automatically.
4. **Startup Execution**: Flyway runs automatically on application startup, verifying checksums and applying pending migration scripts.

## Consequences
### Positive
- **Deterministic Deployments**: Ensures database schema evolution is version-controlled and repeatable across environments.
- **Multi-Dialect Compatibility**: Standardizes migration scripts for PostgreSQL, MySQL, Oracle, MSSQL, and DB2.
- **Audit History**: Tracks applied migrations in `flyway_schema_history`.

### Negative
- **Strict Checksum Enforcements**: Manual alterations to applied SQL scripts break Flyway checksum validation and require repair scripts.
