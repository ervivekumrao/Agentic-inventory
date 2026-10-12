package com.smartinventory.inventory.kafka;

import java.time.Instant;

public class StockUpdateEvent {
    private String sku;
    private Integer quantity;
    private String eventType;
    private Instant timestamp = Instant.now();

    public StockUpdateEvent() {}

    public StockUpdateEvent(String sku, Integer quantity, String eventType) {
        this.sku = sku;
        this.quantity = quantity;
        this.eventType = eventType;
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
