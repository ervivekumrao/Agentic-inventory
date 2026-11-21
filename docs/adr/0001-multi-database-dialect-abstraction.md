# ADR 0001: Multi-Database Dialect Abstraction Framework

## Status
Approved (PostgreSQL & MySQL Primary; Dialect-Specific Migration Scripts Required for Oracle/SQL Server/DB2)

## Context
The `inventory-service` must support enterprise deployments across five major relational database engines: PostgreSQL, MySQL, Microsoft SQL Server, Oracle, and IBM DB2. Each database engine uses distinct vendor-specific SQL syntax for pagination, locking, and string manipulation (e.g., PostgreSQL `LIMIT/OFFSET`, Oracle `FETCH NEXT`, DB2 `FETCH FIRST`, SQL Server `OFFSET FETCH`). Relying exclusively on standard Hibernate JPA forces generic SQL generation that lacks database-specific performance optimizations and creates commercial licensing lock-in concerns.

## Decision
We implemented a 100% permissively licensed (Apache 2.0) polymorphic **Database Query Dialect Abstraction Engine** under `com.smartinventory.inventory.db.dialect`:

1. **Dialect Interface Contract (`QueryDialect`)**: Defines vendor-agnostic methods for SQL query generation, pagination wrapping, and database metadata inspection.
2. **Abstract Base Implementation (`AbstractQueryDialect`)**: Handles common Spring JDBC `JdbcTemplate` and QueryDSL SQL abstractions.
3. **Vendor Implementations**:
   - `PostgresQueryDialect` (PostgreSQL `LIMIT/OFFSET`)
   - `MySqlQueryDialect` (MySQL `LIMIT/OFFSET`)
   - `OracleQueryDialect` (Oracle 12c+ `FETCH NEXT`)
   - `SqlServerQueryDialect` (SQL Server `OFFSET/FETCH`)
   - `Db2QueryDialect` (IBM DB2 `FETCH FIRST`)
4. **Dynamic Factory (`QueryDialectFactory`)**: Selects the active dialect implementation at application startup based on the environment variable `INVENTORY_DB_TYPE` or configuration property `inventory.database.type`.

## Consequences
### Positive
- **Zero Proprietary Vendor Lock-In**: Complies 100% with Apache License 2.0.
- **Runtime Dialect Switching**: Allows changing target relational databases by simply switching an environment variable (`INVENTORY_DB_TYPE=MYSQL`).
- **Optimal Database Performance**: Executes native SQL pagination constructs per database vendor.

### Negative
- **Maintenance Overhead**: Requires updating vendor dialect classes when new SQL query patterns are introduced.
- **Dual Query Abstraction**: Developers must coordinate QueryDSL SQL dialect queries alongside Spring Data JPA repositories.
