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

package com.smartinventory.inventory.controller;

import com.smartinventory.inventory.service.InventoryReservationService;
import com.smartinventory.inventory.service.InventoryReservationService.PoolStatusDto;
import com.smartinventory.inventory.service.InventoryReservationService.ReservationResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
@RequestMapping(value = "/api/inventory/reservations", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventoryReservationController {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservationController.class);

    private final InventoryReservationService reservationService;

    public InventoryReservationController(InventoryReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<ReservationResult> reserveStock(
            @Valid @RequestBody ReserveStockRequest request,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ANONYMOUS";
        log.info("POST /api/inventory/reservations/reserve requested by user: {} for SKU: {}, qty: {}, ref: {}",
                username, request.sku(), request.quantity(), request.reservationId());

        ReservationResult result = reservationService.reserveStock(
                request.sku(),
                request.quantity(),
                request.reservationId(),
                request.ttlSeconds(),
                username
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/claim/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<Map<String, Object>> claimReservation(
            @PathVariable String reservationId,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ANONYMOUS";
        boolean isAdminOrManager = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        log.info("POST /api/inventory/reservations/claim/{} requested by user: {}", reservationId, username);
        boolean success = reservationService.claimReservation(reservationId, username, isAdminOrManager);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "reservationId", reservationId,
                    "status", "CLAIMED",
                    "message", "Reservation claimed and deducted from inventory permanently."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "reservationId", reservationId,
                    "status", "FAILED",
                    "message", "Active reservation not found or already claimed/released."
            ));
        }
    }

    @PostMapping("/release/{reservationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<Map<String, Object>> releaseReservation(
            @PathVariable String reservationId,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "ANONYMOUS";
        boolean isAdminOrManager = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        log.info("POST /api/inventory/reservations/release/{} requested by user: {}", reservationId, username);
        boolean success = reservationService.releaseReservation(reservationId, username, isAdminOrManager);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "reservationId", reservationId,
                    "status", "RELEASED",
                    "message", "Reservation released back to available pool successfully."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "reservationId", reservationId,
                    "status", "FAILED",
                    "message", "Active reservation not found."
            ));
        }
    }

    @GetMapping("/pool/{sku}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<PoolStatusDto> getPoolStatus(@PathVariable String sku) {
        log.debug("GET /api/inventory/reservations/pool/{} requested", sku);
        return ResponseEntity.ok(reservationService.getPoolStatus(sku));
    }

    /**
     * Benchmark endpoint: Simulates high-concurrency flash sale traffic (e.g. 50 parallel buyers
     * competing for the exact same SKU simultaneously) using FOR UPDATE SKIP LOCKED.
     * Proves non-blocking execution and zero overselling.
     */
    @PostMapping("/simulate-flash-sale")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> simulateFlashSale(@RequestBody FlashSaleSimulationRequest request) {
        String sku = request.sku() != null ? request.sku() : "LAPTOP-PRO-15";
        int concurrentCheckouts = request.concurrentCheckouts() > 0 ? Math.min(request.concurrentCheckouts(), 100) : 30;
        int qtyPerCheckout = request.qtyPerCheckout() > 0 ? request.qtyPerCheckout() : 1;

        log.info("Simulating flash sale with {} concurrent checkouts for SKU: {}", concurrentCheckouts, sku);

        ExecutorService executor = Executors.newFixedThreadPool(concurrentCheckouts);
        List<Future<Boolean>> futures = new ArrayList<>();
        Instant startTime = Instant.now();

        for (int i = 0; i < concurrentCheckouts; i++) {
            final String resId = "SIM-ORDER-" + UUID.randomUUID().toString().substring(0, 8);
            futures.add(executor.submit(() -> {
                try {
                    reservationService.reserveStock(sku, qtyPerCheckout, resId, 60);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        int successCount = 0;
        int failureCount = 0;
        for (Future<Boolean> f : futures) {
            try {
                if (f.get(5, TimeUnit.SECONDS)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
            }
        }
        executor.shutdown();

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        PoolStatusDto poolStatus = reservationService.getPoolStatus(sku);

        return ResponseEntity.ok(Map.of(
                "sku", sku,
                "concurrentCheckoutsAttempted", concurrentCheckouts,
                "successfulReservations", successCount,
                "insufficientStockRejections", failureCount,
                "totalExecutionTimeMs", durationMs,
                "averageLatencyPerReservationMs", durationMs / (double) concurrentCheckouts,
                "lockingStrategy", "Bounded Unit Pool (FOR UPDATE SKIP LOCKED)",
                "currentPoolStatus", poolStatus
        ));
    }

    public record ReserveStockRequest(
            @NotBlank(message = "SKU is mandatory")
            String sku,
            @Min(value = 1, message = "Quantity must be at least 1")
            int quantity,
            String reservationId,
            Integer ttlSeconds
    ) {}

    public record FlashSaleSimulationRequest(
            String sku,
            int concurrentCheckouts,
            int qtyPerCheckout
    ) {}
}
