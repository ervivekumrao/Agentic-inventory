# Developer Guide & System Documentation

Welcome to the **Smart Inventory Management System** comprehensive developer guide. This document provides in-depth technical details on architecture, database persistence, security, messaging, API reference, AI/RAG agent capabilities, and testing strategies.

---

## 📑 Table of Contents
1. [Architecture & Design Principles](#1-architecture--design-principles)
2. [Database Configuration & Schemas](#2-database-configuration--schemas)
3. [Event-Driven Messaging (Apache Kafka)](#3-event-driven-messaging-apache-kafka)
4. [Security & Authentication](#4-security--authentication)
5. [Complete REST API Reference](#5-complete-rest-api-reference)
6. [Agentic AI Assistant & Forecasting Service](#6-agentic-ai-assistant--forecasting-service)
7. [Testing Strategy & JaCoCo Coverage](#7-testing-strategy--jacoco-coverage)
8. [Docker Containerization & Orchestration](#8-docker-containerization--orchestration)
9. [Troubleshooting & FAQ](#9-troubleshooting--faq)

---

## 1. Architecture & Design Principles

The system adopts a **polyglot microservices architecture** built on solid object-oriented design and enterprise SOLID design principles:
- **Single Responsibility Principle (SRP)**: Controllers handle HTTP routing/validation; Services encapsulate domain business rules; Repositories manage data access.
- **Open/Closed Principle (OCP)**: Microservice components and DTO mappings are open for extension through modular design and Jackson alias annotations without modifying core entities.
- **Liskov Substitution Principle (LSP)**: JPA repositories extend `JpaRepository` and Mongo repositories extend `MongoRepository` consistently.
- **Interface Segregation Principle (ISP)**: Focused repository interfaces (`InventoryRepository`, `StockTransactionRepository`, `ProductRepository`).
- **Dependency Inversion Principle (DIP)**: Strict Spring Constructor Dependency Injection across all services and controllers.
- **Twelve-Factor App Configuration**: Externalized configuration via `application.yml` and environment variables (`FORECASTING_SERVICE_URL`, `CATALOG_SERVICE_URL`, `SPRING_DATASOURCE_URL`).

---

## 2. Database Configuration & Schemas

### PostgreSQL (`inventory-service`)
- **Default Port**: `5432`
- **Database Name**: `inventory`
- **Connection Pooling**: **HikariCP** (configured with max pool size 10, idle timeout 30s, connection timeout 20s)
- **Entities & Tables**:
  - `inventory_items`: `id` (PK), `sku` (unique index), `quantity`, `reorder_threshold`, `warehouse_location`, `updated_at`.
  - `stock_transactions`: `id` (PK), `sku`, `quantity_change`, `transaction_type` (`INITIAL`, `IN`, `OUT`, `ADJUSTMENT`), `reference_id`, `timestamp`.

### MongoDB (`catalog-service`)
- **Default Port**: `27017`
- **Database Name**: `catalog`
- **Document Model**:
  - `products`: `id` (ObjectId), `sku` (unique index), `name`, `description`, `category`, `price`, `attributes` (dynamic key-value Map), `created_at`, `updated_at`.

---

## 3. Event-Driven Messaging (Apache Kafka)

- **Broker**: Apache Kafka (`localhost:9092`)
- **Topic**: `inventory-stock-events`
- **Producer**: `KafkaProducerService` in `inventory-service` publishes JSON-serialized `StockUpdateEvent` payloads whenever inventory items are created or stock quantities are adjusted.
- **Payload Structure**:
  ```json
  {
    "sku": "SKU-001",
    "quantity": 150,
    "eventType": "IN",
    "timestamp": "2025-01-15T10:00:00Z"
  }
  ```

---

## 4. Security & Authentication

- **Framework**: Spring Security 6 configured in `inventory-service`.
- **Authentication Scheme**: HTTP Basic Authentication.
- **Default Credentials**: 
  - Username: `admin`
  - Password: `password` (encoded with `{noop}`)
- **Security Rules**:
  - **Public Endpoints**: `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api/ai/**`, `/`, `/*.html`, `/*.css`, `/*.js`.
  - **Protected Endpoints**: `/api/inventory/**` require valid HTTP Basic authentication credentials.

---

## 5. Complete REST API Reference

### Inventory Service (`http://localhost:8081`)
- `POST /api/ai/agent/chat` - AI Agent chat endpoint (automatically loads database items and generates human-like responses)
- `GET /api/inventory` - Retrieve all inventory items (Optional query param: `?lowStock=true`)
- `GET /api/inventory/{sku}` - Retrieve inventory details for a SKU
- `POST /api/inventory` - Create inventory item *(Requires Authentication)*
- `POST /api/inventory/{sku}/adjust` - Adjust stock quantity (In/Out/Adjustment) *(Requires Authentication)*
- `GET /api/inventory/{sku}/transactions` - Retrieve audit transaction history for a SKU
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **Web Chat Workspace**: `http://localhost:8081/` or `http://localhost:8081/chat.html`

### Catalog Service (`http://localhost:8082`)
- `GET /api/catalog` - Retrieve all products (Optional query param: `?category=Electronics`)
- `GET /api/catalog/{id}` - Retrieve product by database ID
- `GET /api/catalog/sku/{sku}` - Retrieve product by SKU
- `POST /api/catalog` - Create a new product (Validated)
- `PUT /api/catalog/{id}` - Update product metadata
- `DELETE /api/catalog/{id}` - Delete product record
- **Swagger UI**: `http://localhost:8082/swagger-ui.html`

---

## 6. Agentic AI Assistant & Forecasting Service (`http://localhost:8083`)

- **Tech Stack**: Python 3.11, FastAPI, NumPy, Pandas, Pydantic, HTTPX.
- **Endpoints**:
  - `POST /api/ai/agent/chat`: **Agentic AI Assistant** that ingests inventory and catalog context, analyzes stock levels, calculates reorder deficits and inventory monetary valuation, maps warehouse distributions, detects supply chain risks, renders **Markdown tables**, and generates human-like responses formatted as a Senior Operations Lead.
  - `POST /forecast`: Computes time-series demand projections over a given horizon using exponential smoothing and historical sales data.
- **Interactive Docs**: `http://localhost:8083/docs`

---

## 7. Testing Strategy & JaCoCo Coverage

The project employs a comprehensive multi-layered testing strategy targeting complete test coverage:
- **Unit Tests**: Co-located in each service (`src/test` and `app/test_*.py`), covering controllers, services, repositories, DTOs, models, Kafka producers, exception handlers, and AI agent methods.
- **Integration Tests**: Centralized in `:integration-tests`, validating end-to-end component wiring, security, and context hydration.
- **JaCoCo Test Coverage Reports**: JaCoCo code coverage plugin configured across Gradle subprojects.

### Running Tests & Generating JaCoCo Reports
```bash
# Run all unit & integration tests
./gradlew test

# Generate JaCoCo coverage reports
./gradlew jacocoTestReport
```

JaCoCo HTML reports are generated at:
- `catalog-service/build/reports/jacoco/test/html/index.html`
- `inventory-service/build/reports/jacoco/test/html/index.html`

---

## 8. Docker Containerization & Orchestration

Each service features an optimized Dockerfile:
- **Java Services**: Multi-stage Eclipse Temurin JDK 17 build.
- **Forecasting Service**: Python 3.11 slim image.
- **Docker Compose**: Orchestrates PostgreSQL, MongoDB, Zookeeper, Kafka, and all three microservices via [`infra/docker-compose.dev.yml`](file:///home/user/work/smart-inventory-gradle-java/infra/docker-compose.dev.yml).

To run the full stack:
```bash
docker-compose -f infra/docker-compose.dev.yml up --build -d
```

---

## 9. Troubleshooting & FAQ

- **Port Conflicts**: Ensure ports `5432`, `27017`, `9092`, `8081`, `8082`, and `8083` are free.
- **Java Version**: Ensure JDK 17+ is active in your terminal (`java -version`).
- **Gradle Build Issues**: Run `./gradlew clean build` to refresh build artifacts.
