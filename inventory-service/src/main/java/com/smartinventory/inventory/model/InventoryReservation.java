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
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Represents an authoritative reservation header tracking the full lifecycle
 * (PENDING_PAYMENT, COMPLETED, FAILED, EXPIRED) with strict idempotency.
 */
@Entity
@Table(name = "inventory_reservations", indexes = {
    @Index(name = "idx_res_sku_status", columnList = "sku, status"),
    @Index(name = "idx_res_status_expires", columnList = "status, expiresAt")
})
public class InventoryReservation {

    public static final String STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @Column(nullable = false, length = 128, unique = true)
    @NotBlank(message = "Reservation ID is mandatory")
    private String reservationId;

    @Column(nullable = false)
    @NotBlank(message = "SKU is mandatory")
    private String sku;

    @Column(nullable = false)
    @NotNull(message = "Quantity is mandatory")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Column(nullable = false, length = 32)
    @NotNull(message = "Status is mandatory")
    private String status = STATUS_PENDING_PAYMENT;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant claimedAt;

    @Column(length = 255)
    private String paymentReference;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    private Long version;

    public InventoryReservation() {}

    public InventoryReservation(String reservationId, String sku, Integer quantity, Instant expiresAt) {
        this.reservationId = reservationId;
        this.sku = sku;
        this.quantity = quantity;
        this.status = STATUS_PENDING_PAYMENT;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getClaimedAt() { return claimedAt; }
    public void setClaimedAt(Instant claimedAt) { this.claimedAt = claimedAt; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public void markCompleted(String paymentRef) {
        this.status = STATUS_COMPLETED;
        this.claimedAt = Instant.now();
        this.paymentReference = paymentRef;
    }

    public void markFailed() {
        this.status = STATUS_FAILED;
    }

    public void markExpired() {
        this.status = STATUS_EXPIRED;
    }
}
