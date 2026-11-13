# ADR 0007: Database Pushdown Aggregation & Resilient Catalog Cache Layer

## Status
Approved

## Context
Previously, AI context hydration fetched up to 250 raw inventory records via `findTop250By()`. On large databases (e.g. 100,000 SKUs), this truncated 99%+ of records, causing inaccurate financial valuation reports. Attempting to fetch all raw records into memory caused Out-Of-Memory (OOM) heap crashes and network payload bloat. Furthermore, if `catalog-service` (MongoDB) became temporarily unavailable, price lookups defaulted to $0.00, causing zero-valuation financial reports.

## Decision
We implement **Database Pushdown Aggregations** and a **Resilient Catalog Cache**:

1. **SQL/MongoDB Pushdown Projections**: Push mathematical aggregations (`SUM(valuation)`, `COUNT(low_stock)`, `GROUP BY warehouse`) down to PostgreSQL/RDBMS and MongoDB via interface projections (`InventorySummaryProjection`).
2. **Aggregated Portfolio Summaries**: Send pre-aggregated statistical summaries to `forecasting-service` alongside specific SKU lookups, eliminating raw record memory bloat and payload size limits.
3. **Catalog Resilience Cache (`CatalogCacheService`)**: Introduce a time-expiring in-memory cache backed by `StampedLock` for catalog pricing, categories, and titles. If `catalog-service` is unreachable, `inventory-service` automatically falls back to cached metadata instead of outputting $0.00 valuations.

## Consequences
### Positive
- **100% Database Accuracy**: Portfolio-wide valuations and low-stock counts reflect the entire database, not just a 250-item sample.
- **Zero Memory Bloat**: Payload sizes remain under 5KB regardless of whether the database contains 1,000 or 1,000,000 SKUs.
- **High Availability**: AI workspace remains functional and accurate during transient `catalog-service` outages.

### Negative
- **Cache Invalidation Delay**: Product price changes in MongoDB may take up to the cache TTL duration (e.g. 5 minutes) to reflect in default AI responses unless manually refreshed.
