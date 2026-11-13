# ADR 0003: Transactional Outbox Pattern for Dual-Write ACID & Kafka Consistency

## Status
Approved

## Context
When an inventory stock adjustment occurs, `inventory-service` must perform two operations:
1. Update the database record (`InventoryItem` & `StockTransaction`).
2. Publish an event (`StockUpdateEvent`) to Apache Kafka for downstream microservices.

Executing direct network calls to Kafka inside or outside an ACID database transaction introduces dual-write inconsistencies. If the database commit succeeds but Kafka fails or crashes, downstream microservices miss stock update events. Conversely, if Kafka publishes but the database transaction rolls back, downstream systems receive phantom stock events.

## Decision
We implemented the **Transactional Outbox Pattern** in `InventoryService` and `KafkaProducerService`:

1. **Co-located Outbox Persistence**: When modifying stock, `InventoryService` creates and persists an `OutboxEvent` entity within the **exact same database transaction** as `inventoryRepository.save()`.
2. **Atomic Commit**: Database updates and outbox event creation commit atomically in a single ACID database transaction.
3. **Asynchronous Kafka Publishing**: `KafkaProducerService` handles publishing outbox events to the `inventory-stock-events` Kafka topic.
4. **State Tracking**: Upon successful Kafka acknowledgment, `outboxEvent.markSent()` updates the event status to `SENT`. If Kafka fails, the event is marked `FAILED` for background retry processing.

## Consequences
### Positive
- **Guaranteed At-Least-Once Delivery**: No events are lost even during application crashes or Kafka broker outages.
- **Zero Dual-Write Inconsistency**: Prevents phantom events or database-Kafka state drift.

### Negative
- **Minor Write Overhead**: Adds an extra INSERT statement into the `outbox_events` table during stock mutations.
- **Outbox Table Maintenance**: Requires periodic purging of old `SENT` outbox events.
