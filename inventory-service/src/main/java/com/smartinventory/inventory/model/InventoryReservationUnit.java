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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Represents an individual inventory unit within the bounded pool, enabling
 * non-blocking reservations using database-native FOR UPDATE SKIP LOCKED.
 */
@Entity
@Table(name = "inventory_reservation_units", indexes = {
    @Index(name = "idx_unit_sku_status", columnList = "sku, status"),
    @Index(name = "idx_unit_reservation_id", columnList = "reservationId"),
    @Index(name = "idx_unit_expires_at", columnList = "expiresAt")
})
public class InventoryReservationUnit {

    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CLAIMED = "CLAIMED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "SKU is mandatory")
    private String sku;

    @Column(nullable = false, length = 32)
    @NotNull(message = "Status is mandatory")
    private String status = STATUS_AVAILABLE;

    @Column(length = 128)
    private String reservationId;

    private Instant reservedAt;

    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public InventoryReservationUnit() {}

    public InventoryReservationUnit(String sku) {
        this.sku = sku;
        this.status = STATUS_AVAILABLE;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public Instant getReservedAt() { return reservedAt; }
    public void setReservedAt(Instant reservedAt) { this.reservedAt = reservedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public void reserve(String reservationId, Instant expiresAt) {
        this.status = STATUS_RESERVED;
        this.reservationId = reservationId;
        this.reservedAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void claim() {
        this.status = STATUS_CLAIMED;
    }

    public void release() {
        this.status = STATUS_AVAILABLE;
        this.reservationId = null;
        this.reservedAt = null;
        this.expiresAt = null;
    }
}
