# Database Schemas & Kafka Event Messaging Guide

This document specifies the persistence schemas for PostgreSQL and MongoDB, alongside the Apache Kafka event-driven messaging specifications.

---

## 🐘 1. PostgreSQL Schema (`inventory-service`)

- **Database**: `inventory`
- **ORM / JPA Provider**: Hibernate JPA on Spring Data JPA
- **Connection Pool**: HikariCP (Max pool size: 10, Idle timeout: 30s)

### Table 1: `inventory_items`
Stores active stock items and warehouse locations.

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique surrogate identifier. |
| `sku` | `VARCHAR(255)` | UNIQUE INDEX, NOT NULL | Stock Keeping Unit code. |
| `quantity` | `INTEGER` | NOT NULL | Current physical stock count on hand. |
| `reorder_threshold` | `INTEGER` | NOT NULL | Stock level threshold triggering replenishment. |
| `warehouse_location` | `VARCHAR(255)` | NOT NULL | Facility storage location name. |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last stock update timestamp. |

### Table 2: `stock_transactions`
Stores immutable audit log of all stock adjustments (In, Out, Initial Setup).

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique transaction audit ID. |
| `sku` | `VARCHAR(255)` | NOT NULL | Target SKU code. |
| `quantity_change` | `INTEGER` | NOT NULL | Delta stock change (+ positive or - negative). |
| `transaction_type` | `VARCHAR(50)` | NOT NULL | `INITIAL`, `IN`, `OUT`, `ADJUSTMENT`. |
| `reference_id` | `VARCHAR(255)` | | Associated Purchase Order / Sales Order ID. |
| `timestamp` | `TIMESTAMP` | NOT NULL | Event timestamp. |

---

## 🍃 2. MongoDB Document Schema (`catalog-service`)

- **Database**: `catalog`
- **Collection**: `products`

```json
{
  "_id": {"$oid": "65a000000000000000000001"},
  "sku": "LAPTOP-PRO-15",
  "name": "Pro 15-inch Laptop",
  "description": "High-performance laptop with 16GB RAM and 512GB SSD",
  "category": "Electronics",
  "price": 1299.99,
  "attributes": {
    "brand": "SmartBrand",
    "warranty": "1 Year"
  },
  "createdAt": {"$date": "2025-01-15T10:00:00.000Z"},
  "updatedAt": {"$date": "2025-01-15T10:00:00.000Z"}
}
```

---

## 📡 3. Apache Kafka Event Streaming Specs

- **Broker**: `localhost:9092` (Docker service `kafka:9092`)
- **Topic Name**: `inventory-stock-events`
- **Producer Class**: `KafkaProducerService.java` in `inventory-service`

### Event Payload Schema (`StockUpdateEvent`):
```json
{
  "sku": "LAPTOP-PRO-15",
  "quantity": 50,
  "eventType": "IN",
  "timestamp": "2025-01-15T10:30:00Z"
}
```
Whenever an item is created or stock is adjusted via POST `/api/inventory/{sku}/adjust`, a JSON `StockUpdateEvent` is automatically published to Kafka for downstream analytics consumers.

---

## 🗄️ 4. Multi-Database Support & Query Dialect Framework

`inventory-service` features an extensible, 100% permissively licensed (**Apache License 2.0**) database access framework supporting 5 major database engines:
1. **PostgreSQL** (`POSTGRESQL`) - **Default DB**
2. **MySQL** (`MYSQL`)
3. **Oracle** (`ORACLE`)
4. **Microsoft SQL Server** (`MSSQL`)
5. **IBM DB2** (`DB2`)

### Configuration
Configure the active database dialect in `application.yml` or via environment variables:

```yaml
inventory:
  database:
    type: ${INVENTORY_DB_TYPE:POSTGRESQL}
```

If `inventory.database.type` is omitted or left blank, **PostgreSQL** is selected automatically.

### Architecture & Dialect Pattern (`com.smartinventory.inventory.db.dialect`)

```
com.smartinventory.inventory.db/
├── DatabaseType.java                 (Enum representing database dialects)
├── DatabaseQueryGenerator.java       (Facade service)
├── DatabaseTransactionManager.java   (Programmatic transaction manager)
└── dialect/                          (Sub-package for SQL Dialects)
    ├── QueryDialect.java             (Core dialect interface)
    ├── AbstractQueryDialect.java     (Base class for QueryDSL & Spring JDBC)
    ├── QueryDialectFactory.java      (Polymorphic dialect resolver factory)
    ├── PostgresQueryDialect.java
    ├── MySqlQueryDialect.java
    ├── OracleQueryDialect.java
    ├── SqlServerQueryDialect.java
    └── Db2QueryDialect.java
```

### Key Framework Features
- **Polymorphic Dialects (`QueryDialect`)**: Removes monolithic `switch` statements and adheres strictly to the **Open/Closed Principle**. Adding a new database requires only creating a new dialect class (e.g., `MariaDbQueryDialect`).
- **QueryDSL SQL & Spring JDBC Engine**: Powered by `com.querydsl:querydsl-sql:5.1.0` and Spring's `JdbcTemplate`, providing 100% free Apache 2.0 commercial licensing across all supported engines (including commercial databases like Oracle, MSSQL, and DB2).
- **Programmatic Transaction Control (`DatabaseTransactionManager`)**: Provides fine-grained transaction boundary management, custom callbacks, isolation level overrides, and propagation controls (`REQUIRES_NEW`).
- **Pagination & Top-N Custom Syntax**: Automatically formats `LIMIT / OFFSET` (Postgres, MySQL), `OFFSET / FETCH NEXT` (Oracle, MSSQL), or `OFFSET / FETCH FIRST` (DB2).
- **Metadata Endpoint**: `GET /api/inventory/database-info` exposes the active database dialect, Hibernate dialect, default connection URLs, and sample query rendering.
