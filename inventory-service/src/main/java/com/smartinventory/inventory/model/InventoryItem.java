package com.smartinventory.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "inventory_items", indexes = {
    @Index(name = "idx_sku", columnList = "sku", unique = true)
})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "SKU is mandatory")
    private String sku;

    @Column(nullable = false)
    @NotNull(message = "Quantity is mandatory")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @Column(nullable = false)
    @NotNull(message = "Reorder threshold is mandatory")
    @Min(value = 0, message = "Reorder threshold cannot be negative")
    private Integer reorderThreshold = 10;

    @Column(nullable = false)
    private String warehouseLocation;

    @Version
    private Long version;

    private Instant updatedAt = Instant.now();

    public InventoryItem() {}

    public InventoryItem(String sku, Integer quantity, Integer reorderThreshold, String warehouseLocation) {
        this.sku = sku;
        this.quantity = quantity;
        this.reorderThreshold = reorderThreshold;
        this.warehouseLocation = warehouseLocation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getReorderThreshold() { return reorderThreshold; }
    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }

    public String getWarehouseLocation() { return warehouseLocation; }
    public void setWarehouseLocation(String warehouseLocation) { this.warehouseLocation = warehouseLocation; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    @PrePersist
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
