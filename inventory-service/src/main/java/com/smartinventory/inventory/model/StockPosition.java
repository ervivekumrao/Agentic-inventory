/*
 * Copyright (c) 2026 Vivek Umrao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.smartinventory.inventory.model;

import jakarta.persistence.*;

/**
 * Multi-location stock position entity tracking on-hand, reserved, and safety stock
 * for a specific SKU at a specific warehouse/location (ADR-0011).
 */
@Entity
@Table(name = "stock_positions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sku", "location_id", "lot_id"})
})
public class StockPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "lot_id", nullable = false)
    private String lotId = "DEFAULT";

    @Column(name = "on_hand_quantity", nullable = false)
    private Integer onHandQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "safety_stock_quantity", nullable = false)
    private Integer safetyStockQuantity = 0;

    @Version
    private Long version = 0L;

    public StockPosition() {}

    public StockPosition(String sku, String locationId, int onHandQuantity) {
        this(sku, locationId, "DEFAULT", onHandQuantity, 0, 0);
    }

    public StockPosition(String sku, String locationId, String lotId, int onHandQuantity, int reservedQuantity, int safetyStockQuantity) {
        this.sku = sku;
        this.locationId = locationId;
        this.lotId = lotId != null ? lotId : "DEFAULT";
        this.onHandQuantity = Math.max(0, onHandQuantity);
        this.reservedQuantity = Math.max(0, reservedQuantity);
        this.safetyStockQuantity = Math.max(0, safetyStockQuantity);
    }

    /**
     * Available-To-Promise (ATP) = max(0, on_hand - reserved - safety_stock)
     */
    public int getAvailableToPromise() {
        return Math.max(0, onHandQuantity - reservedQuantity - safetyStockQuantity);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }

    public Integer getOnHandQuantity() { return onHandQuantity; }
    public void setOnHandQuantity(Integer onHandQuantity) { this.onHandQuantity = onHandQuantity; }

    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }

    public Integer getSafetyStockQuantity() { return safetyStockQuantity; }
    public void setSafetyStockQuantity(Integer safetyStockQuantity) { this.safetyStockQuantity = safetyStockQuantity; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
