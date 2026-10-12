package com.smartinventory.inventory.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_transactions")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private Integer quantityChange;

    @Column(nullable = false)
    private String transactionType; // IN, OUT, ADJUSTMENT

    private String referenceId;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    public StockTransaction() {}

    public StockTransaction(String sku, Integer quantityChange, String transactionType, String referenceId) {
        this.sku = sku;
        this.quantityChange = quantityChange;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
