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

import com.smartinventory.inventory.model.PurchaseOrder;
import com.smartinventory.inventory.model.Supplier;
import com.smartinventory.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class PurchaseOrderController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderController.class);
    private final PurchaseOrderService purchaseOrderService;

    @Autowired
    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @GetMapping("/po")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<List<PurchaseOrder>> getAllPurchaseOrders() {
        log.debug("GET /api/inventory/po requested");
        return ResponseEntity.ok(purchaseOrderService.getAllPurchaseOrders());
    }

    @GetMapping("/po/{orderNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<PurchaseOrder> getPurchaseOrderByNumber(@PathVariable String orderNumber) {
        log.debug("GET /api/inventory/po/{} requested", orderNumber);
        return purchaseOrderService.getPurchaseOrderByNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/po")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR')")
    public ResponseEntity<PurchaseOrder> createPurchaseOrder(@Valid @RequestBody PurchaseOrder po) {
        log.info("POST /api/inventory/po requested for order: {}", po.getOrderNumber());
        PurchaseOrder created = purchaseOrderService.createPurchaseOrder(po);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/po/{orderNumber}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PurchaseOrder> approvePurchaseOrder(@PathVariable String orderNumber) {
        log.info("POST /api/inventory/po/{}/approve requested", orderNumber);
        PurchaseOrder approved = purchaseOrderService.approvePurchaseOrder(orderNumber);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/po/{orderNumber}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR')")
    public ResponseEntity<PurchaseOrder> receivePurchaseOrder(@PathVariable String orderNumber) {
        log.info("POST /api/inventory/po/{}/receive requested", orderNumber);
        PurchaseOrder received = purchaseOrderService.receivePurchaseOrder(orderNumber);
        return ResponseEntity.ok(received);
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ENTRY_OPERATOR', 'USER')")
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        log.debug("GET /api/inventory/suppliers requested");
        return ResponseEntity.ok(purchaseOrderService.getAllSuppliers());
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody Supplier supplier) {
        log.info("POST /api/inventory/suppliers requested for supplier code: {}", supplier.getCode());
        Supplier created = purchaseOrderService.createSupplier(supplier);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
