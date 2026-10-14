<!--
  Copyright (c) 2026 Vivek Umrao.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Database Schemas & Kafka Event Messaging Guide

This document specifies the persistence schemas for the multi-database relational persistence layer (PostgreSQL, MySQL, Oracle, MSSQL, DB2) and MongoDB, alongside the Apache Kafka event-driven messaging architecture.

---

## 🐘 1. Relational Persistence Schema (`inventory-service`)

- **Default Database**: PostgreSQL 15 (`inventory`)
- **Supported Engines**: PostgreSQL, MySQL, Oracle, Microsoft SQL Server, IBM DB2
- **ORM / Persistence**: Hibernate JPA on Spring Data JPA, supplemented by Spring `JdbcTemplate` and QueryDSL SQL
- **Connection Pool**: HikariCP (Max pool size: 10, Idle timeout: 30s, Connection timeout: 20s)

### Table 1: `inventory_items`
Stores active stock items and warehouse locations.

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT / IDENTITY | Unique surrogate identifier. |
| `sku` | `VARCHAR(255)` | UNIQUE INDEX, NOT NULL | Stock Keeping Unit code. |
| `quantity` | `INTEGER` | NOT NULL | Current physical stock count on hand. |
| `reorder_threshold` | `INTEGER` | NOT NULL | Stock level threshold triggering replenishment. |
| `warehouse_location` | `VARCHAR(255)` | NOT NULL | Facility storage location name. |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last stock update timestamp. |

### Table 2: `stock_transactions`
Stores immutable audit log of all stock adjustments (In, Out, Initial Setup).

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT / IDENTITY | Unique transaction audit ID. |
| `sku` | `VARCHAR(255)` | NOT NULL | Target SKU code. |
| `quantity_change` | `INTEGER` | NOT NULL | Delta stock change (+ positive or - negative). |
| `transaction_type` | `VARCHAR(50)` | NOT NULL | `INITIAL`, `IN`, `OUT`, `ADJUSTMENT`. |
| `reference_id` | `VARCHAR(255)` | NULLABLE | Associated Purchase Order / Sales Order ID. |
| `timestamp` | `TIMESTAMP` | NOT NULL | Event timestamp. |

### Table 3: `users`
Stores system accounts for Role-Based Access Control (RBAC).

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique user identifier. |
| `username` | `VARCHAR(255)` | UNIQUE INDEX, NOT NULL | Login username. |
| `password` | `VARCHAR(255)` | NOT NULL | BCrypt encrypted password hash. |
| `enabled` | `BOOLEAN / SMALLINT` | NOT NULL | Account active status flag. |
| `created_at` | `TIMESTAMP` | NOT NULL | User registration timestamp. |
| `updated_at` | `TIMESTAMP` | NOT NULL | Last modification timestamp. |

### Table 4: `roles`
Stores distinct administrative and operational security roles.

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique role identifier. |
| `name` | `VARCHAR(255)` | UNIQUE INDEX, NOT NULL | Role name (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_ENTRY_OPERATOR`, `ROLE_USER`). |
| `description` | `VARCHAR(255)` | NULLABLE | Human-readable role description. |

### Table 5: `permissions`
Stores fine-grained system action permissions.

| Column | SQL Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | PRIMARY KEY, AUTO_INCREMENT | Unique permission identifier. |
| `name` | `VARCHAR(255)` | UNIQUE INDEX, NOT NULL | Permission string (`READ_INVENTORY`, `WRITE_INVENTORY`, `ADJUST_STOCK`, etc.). |
| `description` | `VARCHAR(255)` | NULLABLE | Permission details. |

### Join Tables: `user_roles` and `role_permissions`
- `user_roles`: `user_id` (BIGINT, FK -> `users.id`), `role_id` (BIGINT, FK -> `roles.id`).
- `role_permissions`: `role_id` (BIGINT, FK -> `roles.id`), `permission_id` (BIGINT, FK -> `permissions.id`).

---

## 🗃️ 2. Multi-Database Query Dialect Framework

`inventory-service` supports 5 relational database engines via the [`QueryDialect`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/dialect/QueryDialect.java) polymorphic framework:

### Dialect Pagination Syntax Variations
| Database Engine | Dialect Implementation | Pagination Syntax | Driver & Notes |
| :--- | :--- | :--- | :--- |
| **PostgreSQL** | `PostgresQueryDialect` | `LIMIT {limit} OFFSET {offset}` | `org.postgresql:postgresql` |
| **MySQL** | `MySqlQueryDialect` | `LIMIT {offset}, {limit}` | `com.mysql:mysql-connector-j` |
| **Oracle** | `OracleQueryDialect` | `OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY` | `com.oracle.database.jdbc:ojdbc11` |
| **Microsoft SQL Server** | `SqlServerQueryDialect` | `ORDER BY id OFFSET {offset} ROWS FETCH NEXT {limit} ROWS ONLY` | `com.microsoft.sqlserver:mssql-jdbc` |
| **IBM DB2** | `Db2QueryDialect` | `OFFSET {offset} ROWS FETCH FIRST {limit} ROWS ONLY` | `com.ibm.db2:jcc` |

---

## 🍃 3. MongoDB Document Schema (`catalog-service`)

- **Database**: `catalog`
- **Collection**: `products`
- **Spring Data Mongo Repository**: [`ProductRepository.java`](file:///home/user/work/Agentic-inventory/catalog-service/src/main/java/com/smartinventory/catalog/repository/ProductRepository.java)

### Sample Document Structure:
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
    "warranty": "1 Year",
    "ram": "16GB",
    "storage": "512GB SSD"
  },
  "createdAt": {"$date": "2025-01-15T10:00:00.000Z"},
  "updatedAt": {"$date": "2025-01-15T10:00:00.000Z"}
}
```

### Collection Indexes:
- `_id`: Default MongoDB ObjectID index.
- `sku`: Unique sparse index enforcing SKU uniqueness across the catalog.
- `category`: B-Tree index accelerating category filtering queries.

---

## 📡 4. Apache Kafka Event Streaming Specifications

- **Broker**: `localhost:9092` (Docker service `kafka:9092`)
- **Topic Name**: `inventory-stock-events`
- **Partitions**: Configured with partition key = `sku` to guarantee FIFO ordering per SKU.
- **Producer Class**: [`KafkaProducerService.java`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/kafka/KafkaProducerService.java) in `inventory-service`.

### Event Payload Schema (`StockUpdateEvent`):
```json
{
  "sku": "LAPTOP-PRO-15",
  "quantity": 50,
  "eventType": "IN",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### Event Lifecycle:
1. Client submits stock change (`POST /api/inventory/{sku}/adjust` or `POST /api/inventory`).
2. `InventoryService` persists stock delta and records immutable `StockTransaction`.
3. `KafkaProducerService` formats and publishes `StockUpdateEvent` to Kafka asynchronously.
4. Downstream analytical services or forecasting pipelines consume events to recalibrate predictive demand models in real time.

---

## 📜 5. License & Copyright

```text
Copyright (c) 2026 Vivek Umrao.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
