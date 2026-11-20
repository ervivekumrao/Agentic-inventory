# Architecture Decision Records (ADRs)

This directory documents key architectural decisions made for the **Smart Inventory Management System**.

## Decision Log Index

| ADR ID | Title | Status | Date |
| :--- | :--- | :---: | :--- |
| **[ADR-0001](0001-multi-database-dialect-abstraction.md)** | Multi-Database Dialect Abstraction Framework (Postgres, MySQL, Oracle, MSSQL, DB2) | Approved | 2026-05-15 |
| **[ADR-0002](0002-skip-locked-inventory-reservations.md)** | High-Scale Inventory Reservations via `FOR UPDATE SKIP LOCKED` | Approved | 2026-05-15 |
| **[ADR-0003](0003-transactional-outbox-event-streaming.md)** | Transactional Outbox Pattern for Dual-Write ACID & Kafka Consistency | Approved | 2026-05-15 |
| **[ADR-0004](0004-polyglot-ai-agent-architecture.md)** | Polyglot Dual-Mode AI Agent (Local Scikit-Learn ML + External Cloud LLMs) | Approved | 2026-05-15 |
| **[ADR-0005](0005-flyway-schema-migration.md)** | Deterministic Schema Migration with Flyway Across SQL Dialects | Approved | 2026-05-15 |
| **[ADR-0006](0006-virtual-threads-and-stampedlock-concurrency.md)** | High-Concurrency Virtual Threads (Project Loom) & StampedLock Engine | Approved | 2026-05-20 |
| **[ADR-0007](0007-pushdown-aggregation-and-catalog-cache-resilience.md)** | Database Aggregation Pushdown & Resilient Catalog Cache Layer | Approved | 2026-05-20 |
| **[ADR-0008](0008-seasonal-forecasting-and-runtime-config.md)** | Seasonal Holt-Winters Forecasting & Zero-Downtime Dynamic Runtime Config | Approved | 2026-05-20 |
| **[ADR-0009](0009-centralized-algorithm-agnostic-hashing-framework.md)** | Centralized Algorithm-Agnostic Hashing Framework | Approved | 2026-05-20 |
| **[ADR-0010](0010-pgvector-interaction-learning-feedback-loop.md)** | pgvector Interaction Learning Feedback Loop & Active Learning Engine | Approved | 2026-05-20 |
| **[ADR-0011](0011-multi-location-stock-positions-and-hot-sku-bucketed-counters.md)** | Multi-Location Stock Positions and Hot-SKU Bucketed Counter Architecture | Proposed | 2026-11-19 |
