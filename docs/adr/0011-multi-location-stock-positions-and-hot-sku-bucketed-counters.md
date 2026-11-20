# ADR 0011: Multi-Location Stock Positions and Hot-SKU Bucketed Counter Architecture

## Status
Proposed / Under Owner Review (Tier 3 Design Decision)

## Context
The current inventory reservation system relies on a single master item record (`inventory_items`) per SKU and a bounded unit pool (`inventory_reservation_units`). While `FOR UPDATE SKIP LOCKED` on discrete unit rows prevents row lock serialization during flash sales, this model exhibits three core production limitations at enterprise scale:

1. **Single Location Assumption**: A SKU currently maps to exactly one `warehouseLocation`. Modern fulfillment networks require multi-location stock positions (warehouses, retail stores, fulfillment centers, and lot/batch tracking).
2. **High Row Count Overhead for Fungible Goods**: Representing millions of identical fungible items as individual `InventoryReservationUnit` database rows creates table bloat, index maintenance overhead, and JDBC batch insertion penalties during pool replenishment.
3. **Master Ledger Contention on Final Claim**: When payment callbacks finalize reservations, updating a single `inventory_items.quantity` counter row creates optimistic locking retries (`@Version` failures) or row lock waits under high concurrent claim volume.

## Decision
We propose transitioning the inventory engine to a **Multi-Location Stock Position & Bucketed Counter Architecture**:

### 1. Multi-Location Stock Position Data Model (`stock_positions`)
Replace the single master item counter with granular stock positions keyed by `(sku, location_id, lot_id)`:
- `on_hand_quantity`: Total physical units present in the warehouse/location.
- `reserved_quantity`: Units allocated to active pending reservations (`PENDING_PAYMENT`).
- `safety_stock_quantity`: Buffer stock protected from online reservation.
- `available_to_promise` ($ATP$): Derived as $\max(0, \text{on\_hand\_quantity} - \text{reserved\_quantity} - \text{safety\_stock\_quantity})$.
- `StockTransaction`: Retained as an immutable append-only movement ledger with unique `idempotency_key` and `transaction_type` (`RESERVATION_CLAIM`, `STOCK_ADJUSTMENT`, `WAREHOUSE_TRANSFER`).

### 2. Differentiated Concurrency Strategy
- **Serialized & Unique Goods** (e.g. seats, tickets, device IMEIs): Retain discrete unit rows (`inventory_reservation_units`) with specific item attributes (`serial_number`, `seat_id`).
- **Fungible & Bulk Goods** (e.g. apparel, consumer electronics): Use atomic counter updates directly on stock positions rather than instantiating individual unit rows.

### 3. Hot-SKU Counter Bucketing (`stock_position_buckets`)
To eliminate single-row lock contention on ultra-popular flash-sale SKUs, split a SKU's stock position at a location into $K$ parallel bucket rows (`bucket_id` from $0$ to $K-1$):

```text
                  +-----------------------------------+
                  |   Hot SKU Flash Sale Item         |
                  +-----------------------------------+
                                    |
          +-------------------------+-------------------------+
          |                         |                         |
+-------------------+     +-------------------+     +-------------------+
| Bucket 0          |     | Bucket 1          |     | Bucket K-1        |
| reserved = 42     |     | reserved = 38     |     | reserved = 45     |
| ATP = 58          |     | ATP = 62          |     | ATP = 55          |
+-------------------+     +-------------------+     +-------------------+
```

#### Reservation Protocol
1. **Bucket Selection**: The client/service selects an initial bucket index $b = \text{hash}(\text{client\_id} + \text{timestamp}) \pmod K$.
2. **Atomic Reservation**: Execute an atomic SQL update against bucket $b$:
   ```sql
   UPDATE stock_position_buckets
   SET reserved_quantity = reserved_quantity + :qty
   WHERE sku = :sku
     AND location_id = :locationId
     AND bucket_id = :b
     AND (on_hand_quantity - reserved_quantity - safety_stock_quantity) >= :qty;
   ```
3. **Overflow Probing**: If 0 rows are updated (bucket $b$ exhausted), probe adjacent buckets $(b+1) \pmod K, (b+2) \pmod K$ up to $N$ attempts before reporting SKU out of stock.
4. **Background Rebalancing**: A periodic background job redistributes unreserved stock evenly across buckets $0..K-1$ to prevent localized bucket depletion.

---

### 4. Comparative Architectural Evaluation

| Evaluation Dimension | (A) Discrete Unit Rows (Current) | (B) Single Master Atomic Counter | (C) Hot-SKU Bucketed Counters (Proposed) |
| :--- | :--- | :--- | :--- |
| **Row Count Overhead** | High ($O(N)$ rows for $N$ units) | Minimal ($1$ row per SKU) | Low ($K$ rows per SKU/location) |
| **Single-Row Lock Contention** | None at reserve time (`SKIP LOCKED`); High on master claim | High at peak reserve/claim | Divided by $K$ ($K$-way parallelism) |
| **Throughput Capacity** | ~1,000–2,500 req/sec | ~500–1,000 req/sec (DB CPU bound) | **10,000+ req/sec** |
| **Implementation Complexity** | Medium (requires reaper & pool refill) | Low (single SQL update) | Medium-High (requires bucket router & rebalancer) |
| **Serialized Items Support** | Excellent (row per serial number) | Poor | Medium (requires sub-unit mapping) |

---

### 5. Migration Strategy & Phased Rollout

1. **Phase 1 (Dual-Write & Schema Update)**:
   - Deploy `stock_positions` and `stock_position_buckets` tables alongside `inventory_items`.
   - Enable dual-writing on stock adjustments and purchases.
2. **Phase 2 (Hot-SKU Opt-In)**:
   - Enable bucketed counters ($K=10$ to $K=50$) for high-volume flash-sale SKUs via dynamic feature flag (`inventory.bucketed-skus=LAPTOP-PRO-15,PHONE-FLAGSHIP`).
   - Standard low-volume SKUs continue using single position records ($K=1$).
3. **Phase 3 (Legacy Deprecation)**:
   - Migrate all $ATP$ calculations to `stock_positions` multi-location queries and deprecate `inventory_items.quantity`.

---

## Consequences

### Positive
- **Linear Scaling**: Provides $K$-way parallel throughput for flash-sale SKUs, scaling beyond 10,000 reservations/sec without database lock contention.
- **Multi-Warehouse Support**: Native support for per-warehouse, per-store, and per-lot inventory management ($ATP$).
- **Reduced DB Bloat**: Eliminates the need to maintain millions of `inventory_reservation_units` rows for fungible items.

### Negative
- **Bucket Rebalancing Overhead**: Requires a scheduled background job to keep buckets balanced during asymmetric demand spikes.
- **Slightly Complex Querying**: Aggregating total stock across $K$ buckets requires a `SUM()` query across `stock_position_buckets`.
