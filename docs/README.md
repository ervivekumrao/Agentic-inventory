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

# Developer Guide & System Documentation

Welcome to the **Smart Inventory Management System** developer guide and technical reference. This manual provides in-depth technical details on system architecture, database persistence, security, event streaming, distributed tracing, AI/RAG agent capabilities, frontend workspaces, and testing strategies.

---

## 📑 Table of Contents
1. [Architecture & Design Principles](#1-architecture--design-principles)
2. [Multi-Database Persistence & Dialect Abstraction](#2-multi-database-persistence--dialect-abstraction)
3. [Product Catalog & MongoDB Document Model](#3-product-catalog--mongodb-document-model)
4. [Enterprise Security & RBAC](#4-enterprise-security--rbac)
5. [Distributed Tracing & Correlation ID Architecture](#5-distributed-tracing--correlation-id-architecture)
6. [Dynamic Observability & Logging Admin UI](#6-dynamic-observability--logging-admin-ui)
7. [Event-Driven Messaging with Apache Kafka](#7-event-driven-messaging-with-apache-kafka)
8. [Agentic AI Assistant & Demand Forecasting Engine](#8-agentic-ai-assistant--demand-forecasting-engine)
9. [Web Chat Workspace & Multi-Format Export Engine](#9-web-chat-workspace--multi-format-export-engine)
10. [Complete REST API Reference](#10-complete-rest-api-reference)
11. [Testing Strategy, Testcontainers & JaCoCo Coverage](#11-testing-strategy-testcontainers--jacoco-coverage)
12. [Podman, Docker & Kubernetes Helm Deployment](#12-podman-docker--kubernetes-helm-deployment)
13. [Troubleshooting & Developer FAQ](#13-troubleshooting--developer-faq)
14. [License & Copyright](#14-license--copyright)

---

## 1. Architecture & Design Principles

The system adopts a **polyglot microservices architecture** built on solid object-oriented design and enterprise SOLID principles:
- **Single Responsibility Principle (SRP)**: Controllers strictly route HTTP requests; Services encapsulate domain business rules; Repositories manage data persistence; Dialect classes generate vendor-specific SQL queries.
- **Open/Closed Principle (OCP)**: Microservice components, DTO mappings, and database dialect strategies are open for extension without modifying existing core logic.
- **Liskov Substitution Principle (LSP)**: All dialect strategies extend [`AbstractQueryDialect`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/dialect/AbstractQueryDialect.java) and implement [`QueryDialect`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/dialect/QueryDialect.java) consistently. JPA repositories extend `JpaRepository` and Mongo repositories extend `MongoRepository`.\n- **Interface Segregation Principle (ISP)**: Focused domain repositories ([`InventoryRepository`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/repository/InventoryRepository.java), [`StockTransactionRepository`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/repository/StockTransactionRepository.java), [`ProductRepository`](file:///home/user/work/Agentic-inventory/catalog-service/src/main/java/com/smartinventory/catalog/repository/ProductRepository.java)).
- **Dependency Inversion Principle (DIP)**: Strict Spring Constructor Dependency Injection across all services, controllers, and dialect factories.
- **Twelve-Factor App Configuration**: Externalized configuration via `application.yml` and environment variable overrides (`DB_HOST`, `DB_PORT`, `FORECASTING_SERVICE_URL`, `CATALOG_SERVICE_URL`, `KAFKA_HOST`).

---

## 2. Multi-Database Persistence & Dialect Abstraction

The `inventory-service` features a multi-database access framework supporting 5 enterprise relational database engines under the **Apache License 2.0**:
1. **PostgreSQL** (`POSTGRESQL`) — Default
2. **MySQL** (`MYSQL`)
3. **Oracle** (`ORACLE`)
4. **Microsoft SQL Server** (`MSSQL`)
5. **IBM DB2** (`DB2`)

### Dialect Architecture (`com.smartinventory.inventory.db`)
- [`DatabaseType`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/DatabaseType.java): Enumeration identifying the active database vendor.
- [`QueryDialect`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/dialect/QueryDialect.java): Core dialect contract declaring vendor SQL formatting methods (`buildSelectQuery`, `buildInsertQuery`, `buildUpdateQuery`, `buildDeleteQuery`, `formatPagination`, `getDatabaseInfo`).
- [`QueryDialectFactory`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/dialect/QueryDialectFactory.java): Polymorphic dialect resolver instantiating the appropriate dialect based on configuration.
- [`DatabaseQueryGenerator`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/DatabaseQueryGenerator.java): Facade executing custom SQL statements via Spring's `JdbcTemplate`.
- [`DatabaseTransactionManager`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/db/DatabaseTransactionManager.java): Programmatic transaction controller supporting custom isolation levels and `REQUIRES_NEW` propagation.

### Core Relational Tables
1. **`inventory_items`**:
   - `id` (BIGINT, PK, Auto-increment)
   - `sku` (VARCHAR, Unique Index, NOT NULL)
   - `quantity` (INT, NOT NULL)
   - `reorder_threshold` (INT, NOT NULL)
   - `warehouse_location` (VARCHAR, NOT NULL)
   - `updated_at` (TIMESTAMP, NOT NULL)
2. **`stock_transactions`**:
   - `id` (BIGINT, PK, Auto-increment)
   - `sku` (VARCHAR, NOT NULL)
   - `quantity_change` (INT, NOT NULL)
   - `transaction_type` (VARCHAR, NOT NULL: `INITIAL`, `IN`, `OUT`, `ADJUSTMENT`)
   - `reference_id` (VARCHAR)
   - `timestamp` (TIMESTAMP, NOT NULL)

---

## 3. Product Catalog & MongoDB Document Model

The `catalog-service` manages dynamic product definitions in **MongoDB**:
- **Database**: `catalog`
- **Collection**: `products`
- **Document Model ([`Product.java`](file:///home/user/work/Agentic-inventory/catalog-service/src/main/java/com/smartinventory/catalog/model/Product.java))**:
  ```json
  {
    "id": "65a000000000000000000001",
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
    "createdAt": "2025-01-15T10:00:00Z",
    "updatedAt": "2025-01-15T10:00:00Z"
  }
  ```
- **Category Querying**: Supports full-collection listings and filtered searches via `GET /api/catalog?category=Electronics`.

---

## 4. Enterprise Security & RBAC

The system implements Role-Based Access Control (RBAC) via Spring Security:

### Domain Model
- [`UserEntity`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/model/UserEntity.java): Username, BCrypt-hashed password, enabled flag, created/updated timestamps, many-to-many relationship with roles.
- [`RoleEntity`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/model/RoleEntity.java): Role name (e.g., `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_ENTRY_OPERATOR`, `ROLE_USER`), description, many-to-many relationship with permissions.
- [`PermissionEntity`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/model/PermissionEntity.java): Fine-grained permission strings (e.g., `READ_INVENTORY`, `WRITE_INVENTORY`, `ADJUST_STOCK`, `ADMIN_USERS`).
- [`DatabaseUserDetailsService`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/config/DatabaseUserDetailsService.java): Dynamic user authentication against relational database tables.

### Default Seeded Credentials
| Username | Password | Assigned Role | Capabilities |
| :--- | :--- | :--- | :--- |
| `admin` | `password` | `ROLE_ADMIN` | Full access: User management, actuator, logging admin, stock write/adjust, AI chat. |
| `manager` | `password` | `ROLE_MANAGER` | Stock adjustments, inventory creation, AI chat, read access. |
| `operator` | `password` | `ROLE_ENTRY_OPERATOR` | Stock adjustments, inventory reading, AI chat. |
| `viewer` | `password` | `ROLE_USER` | Read-only inventory access, AI chat. |

### User Management Endpoints (`UserManagementController`)
- `GET /api/admin/users`: List all registered system users (Requires `ROLE_ADMIN`).
- `POST /api/admin/users`: Create a new user with designated roles (Requires `ROLE_ADMIN` or bootstrap).
- `PUT /api/admin/users/{userId}/roles`: Update role assignments for a user (Requires `ROLE_ADMIN`).
- `PUT /api/admin/users/{userId}/status`: Enable/disable user account (Requires `ROLE_ADMIN`).
- `GET /api/admin/roles`: List all available system roles (Requires `ROLE_ADMIN`).

---

## 5. Distributed Tracing & Correlation ID Architecture

To facilitate seamless request tracking across distributed microservices, both Java services incorporate [`CorrelationIdFilter`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/config/CorrelationIdFilter.java):
1. Reads `X-Correlation-ID` HTTP header if provided by client or upstream gateway.
2. Generates a new random UUID if the header is absent.
3. Injects the correlation ID into the SLF4J **MDC** (`Mapped Diagnostic Context`) under key `correlationId`.
4. Attaches the `X-Correlation-ID` header to the outgoing HTTP response.
5. All log statements across `inventory-service` and `catalog-service` include `[correlationId]` in their pattern, enabling instant end-to-end log aggregation in tools like Grafana Loki, ELK, or Datadog.

---

## 6. Dynamic Observability & Logging Admin UI

The system includes a dedicated web-based logging control center at **[`http://localhost:8081/logging-admin.html`](http://localhost:8081/logging-admin.html)**:
- **Secured Access**: Protected by Spring Security, requiring `ROLE_ADMIN` credentials.
- **Real-Time Log Level Inspection**: Interacts with Spring Boot Actuator (`/actuator/loggers`) to display current configured and effective log levels for all system loggers and root packages.
- **Runtime Log Level Modifications**: Dynamically toggle log levels (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`) without requiring container restarts or redeployments.
- **Search & Filtering**: Instant client-side search to locate specific packages (e.g., `com.smartinventory`, `org.hibernate`, `org.apache.kafka`).

---

## 7. Event-Driven Messaging with Apache Kafka

Stock adjustment events are published asynchronously to Apache Kafka for downstream consumers:
- **Broker**: `localhost:9092` (Container service `kafka:9092`)
- **Topic**: `inventory-stock-events`
- **Producer Class**: [`KafkaProducerService.java`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/kafka/KafkaProducerService.java)
- **Event Schema ([`StockUpdateEvent`](file:///home/user/work/Agentic-inventory/inventory-service/src/main/java/com/smartinventory/inventory/kafka/StockUpdateEvent.java))**:
  ```json
  {
    "sku": "LAPTOP-PRO-15",
    "quantity": 50,
    "eventType": "IN",
    "timestamp": "2025-01-15T10:30:00Z"
  }
  ```

---

## 8. Agentic AI Assistant & Demand Forecasting Engine

The `forecasting-service` runs an intelligent supply chain assistant that acts as a virtual **Senior Inventory Operations Manager**:

### Processing Pipeline
1. **Text Preprocessor**: Normalizes user query text and extracts semantic tokens.
2. **Context Analytics Calculator**: Calculates portfolio metrics (total SKUs, total units, gross holding valuation, stockout risk deficits, warehouse allocations).
3. **ML Intent Classifier**: Employs a trained Scikit-Learn TF-IDF vectorizer + Calibrated Logistic Regression model ([`models/intent_classifier.joblib`](file:///home/user/work/Agentic-inventory/forecasting-service/app/models/intent_classifier.joblib)) to classify user queries into 15 operational intents.
4. **Named Entity Recognition (NER)**: Extracts SKUs (e.g., `LAPTOP-PRO-15`), warehouses (`Warehouse-North`), categories (`Electronics`), and numeric filters.
5. **Human Synthesis Engine**: Generates comprehensive responses formatted with markdown tables, risk indicators (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), financial valuations, and actionable next steps.
6. **Dual-Mode LLM Routing**: If `OPENAI_API_KEY` or `GEMINI_API_KEY` is set, passes live context to modern frontier LLMs; otherwise defaults to the local offline synthesis engine.

### Demand Forecasting Formulas (`/forecast`)
- **Exponential Smoothing**: $\hat{y}_{t+1} = \alpha y_t + (1 - \alpha) \hat{y}_t$
- **Safety Stock Formula**: $\text{Safety Stock} = Z \times \sigma_{\text{demand}} \times \sqrt{L}$ ($Z = 1.65$ for 95% service level)
- **Dynamic Reorder Point (ROP)**: $\text{ROP} = (\text{Daily Demand} \times L) + \text{Safety Stock}$

---

## 9. Web Chat Workspace & Multi-Format Export Engine

Served at **[`http://localhost:8081/`](http://localhost:8081/)** or **[`http://localhost:8081/chat.html`](http://localhost:8081/chat.html)**:
- **Interactive Chat Interface**: Dark-themed enterprise UI with real-time response rendering via `marked.js`.
- **Suggestion Chips**: Quick triggers for Tabular SKU Matrix, SKU Count, Stock Type Breakdown, Low Stock Audit, Total Valuation, Executive Brief.
- **Client-Side Document Export Suite**:
  - 📊 **Excel (`.xlsx`)**: Converts rendered HTML tables into native multi-sheet OpenXML spreadsheets using **SheetJS (xlsx.js)**.
  - 📄 **Word (`.docx`)**: Converts conversation bubbles and markdown tables into genuine Microsoft Word `.docx` documents using **docx.js**.
  - 📑 **PDF (`.pdf`)**: Generates high-resolution vector PDF reports via **html2pdf.js**.
  - ⚡ **Smart Export**: Dynamically detects message content type (tabular matrix vs narrative document) and automatically downloads the optimal format.

---

## 10. Complete REST API Reference

### Inventory Service (`http://localhost:8081`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/ai/agent/chat` | AI Agent conversational query | Public |
| `GET` | `/api/inventory` | List all inventory items (`?lowStock=true`) | Public / Authenticated |
| `GET` | `/api/inventory/{sku}` | Retrieve stock details for specific SKU | Public / Authenticated |
| `POST` | `/api/inventory` | Create new inventory item | Yes (`ROLE_ADMIN`, `ROLE_MANAGER`) |
| `POST` | `/api/inventory/{sku}/adjust` | Adjust stock quantity (`IN`, `OUT`, `ADJUSTMENT`) | Yes (`ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_ENTRY_OPERATOR`) |
| `GET` | `/api/inventory/{sku}/transactions` | Retrieve stock audit trail | Public / Authenticated |
| `GET` | `/api/inventory/database-info` | Exposes active SQL dialect & driver metadata | Public |
| `GET` | `/api/admin/users` | List all system users | Yes (`ROLE_ADMIN`) |
| `POST` | `/api/admin/users` | Register new user | Yes (`ROLE_ADMIN` or bootstrap) |
| `PUT` | `/api/admin/users/{userId}/roles` | Update user roles | Yes (`ROLE_ADMIN`) |
| `PUT` | `/api/admin/users/{userId}/status` | Enable/disable user account | Yes (`ROLE_ADMIN`) |
| `GET` | `/api/admin/roles` | List all system roles | Yes (`ROLE_ADMIN`) |
| `GET` | `/actuator/loggers` | Spring Boot Actuator loggers | Yes (`ROLE_ADMIN`) |

### Catalog Service (`http://localhost:8082`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/catalog` | List products (`?category=Electronics`) | Public |
| `GET` | `/api/catalog/{id}` | Get product by MongoDB ID | Public |
| `GET` | `/api/catalog/sku/{sku}` | Get product by SKU code | Public |
| `POST` | `/api/catalog` | Create product | Public |
| `PUT` | `/api/catalog/{id}` | Update product details | Public |
| `DELETE` | `/api/catalog/{id}` | Delete product | Public |

### AI & Forecasting Service (`http://localhost:8083`)
| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/ai/agent/chat` | AI Assistant query endpoint | Public |
| `POST` | `/forecast` | Demand projections & statistical safety stock | Public |

---

## 11. Testing Strategy, Testcontainers & JaCoCo Coverage

The project enforces high code quality and test coverage across all subprojects:
- **Unit Tests**: JUnit 5, Mockito, and AssertJ across controllers, services, repositories, exception handlers, and database dialects.
- **Integration Tests**: Spring Boot Test with Testcontainers spinning up real isolated MongoDB and PostgreSQL containers.
- **Actuator & Security Tests**: Dedicated tests validating security rules and actuator logging endpoints.
- **Coverage Reports**: JaCoCo reports generated at `build/reports/jacoco/test/html/index.html` in each subproject.

Run all tests:
```bash
./gradlew test
```

---

## 12. Podman, Docker & Kubernetes Helm Deployment

### Podman (Default Container Engine)
Podman is the default container engine. Automation scripts ([`scripts/setup.sh`](file:///home/user/work/Agentic-inventory/scripts/setup.sh) and [`scripts/reload_service.sh`](file:///home/user/work/Agentic-inventory/scripts/reload_service.sh)) auto-detect `podman compose`, activate the rootless socket (`podman.socket`), and fall back gracefully to Docker if Podman is not found.

For in-depth details on rootless setup, Testcontainers socket bridging, and Kubernetes YAML generation, see **[Podman Migration Guide](PODMAN_MIGRATION.md)**.

Run the multi-service stack with a specific database:
```bash
# PostgreSQL (Default)
./scripts/setup.sh fresh postgres

# MySQL
./scripts/setup.sh fresh mysql

# Microsoft SQL Server
./scripts/setup.sh fresh mssql

# Oracle
./scripts/setup.sh fresh oracle

# IBM DB2
./scripts/setup.sh fresh db2
```

### Native Kubernetes Pods via Podman (`podman play kube`)
Deploy the multi-service pod directly to Podman's native Kubernetes engine:
```bash
podman play kube infra/podman/smart-inventory-pod.yaml
```

### Kubernetes via Helm (`infra/helm/smart-inventory`)
Deploy to any Kubernetes cluster:
```bash
# Validate chart
helm lint infra/helm/smart-inventory

# Install release
helm install smart-inventory infra/helm/smart-inventory --namespace inventory --create-namespace

# Check rollout status
kubectl get pods -n inventory
```

---

## 13. Troubleshooting & Developer FAQ

### 1. Port Conflicts (e.g., Port 8081 or 5432 already in use)
Run `./scripts/setup.sh destroy` to terminate old containers or adjust ports in `.env` / `docker-compose.dev.yml`.

### 2. Microservice Fails to Connect to Database
Check that your chosen database engine container is fully initialized and healthy. The setup script includes built-in health check polling.

### 3. AI Agent Returns Offline Mock Data
Verify that `forecasting-service` is running on port `8083`. If `inventory-service` cannot reach `forecasting-service`, it falls back gracefully to local Java analytics synthesis without interrupting user workflows.

### 4. Podman Registry Resolution & Health Checks
- **Short Name Resolution**: Podman resolves short image names using `~/.config/containers/registries.conf` (`unqualified-search-registries = ["docker.io"]`), which is automatically created by `./scripts/setup.sh`.
- **Health Checks**: Microservices implement clean standalone healthcheck commands (e.g. [`forecasting-service/healthcheck.py`](file:///home/user/work/Agentic-inventory/forecasting-service/healthcheck.py) and Spring Actuator curl endpoints) preventing whitespace-tokenization syntax errors in container runtimes.

---

## 14. License & Copyright

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
