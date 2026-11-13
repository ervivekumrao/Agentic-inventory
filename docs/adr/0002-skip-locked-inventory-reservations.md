# ADR 0002: High-Scale Inventory Reservations via FOR UPDATE SKIP LOCKED

## Status
Approved

## Context
During peak flash sales or high-concurrency checkout events, hundreds of concurrent customer requests attempt to reserve stock for popular SKUs. Traditional database locking strategies (e.g. pessimistic locking on a single master inventory stock row) serialize requests, leading to row lock contention, database connection pool exhaustion, transaction timeouts, and high error rates.

## Decision
We implemented a bounded unit-pool non-blocking reservation architecture using `FOR UPDATE SKIP LOCKED` in `InventoryReservationService`:

1. **Unit-Pool Ledger Entity (`InventoryReservationUnit`)**: Individual stock units are pre-partitioned into discrete reservation unit rows in the `inventory_reservation_units` table.
2. **Non-Blocking SKIP LOCKED Query**: When a reservation request arrives, `unitRepository.fetchAvailableUnitsForUpdate(sku, quantity)` executes SQL `SELECT ... FOR UPDATE SKIP LOCKED`.
3. **Parallel Execution**: Concurrent transactions skip already-locked unit rows and lock available units immediately without waiting or serializing on a single hot row.
4. **Idempotency & Expiration**: Reservations support client-supplied `reservationId` idempotency keys and automatic TTL expiration cleanup via background scheduled tasks (`@Scheduled`).

## Consequences
### Positive
- **Zero Row Serialization**: Concurrent checkout transactions proceed in parallel without lock wait timeouts.
- **High Concurrency Throughput**: Eliminates database deadlocks and supports thousands of reservations per second.
- **Idempotency & Resilience**: Prevents duplicate reservations when network requests are retried.

### Negative
- **Row Count Growth**: Partitioning stock into discrete units increases total row count in `inventory_reservation_units`.
- **Pool Replenishment Overhead**: Requires inline pool replenishment from the master ledger when available unit pools run low.
