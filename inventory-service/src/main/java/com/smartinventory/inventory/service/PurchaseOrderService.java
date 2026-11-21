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

package com.smartinventory.inventory.service;

import com.smartinventory.inventory.model.PurchaseOrder;
import com.smartinventory.inventory.model.PurchaseOrderItem;
import com.smartinventory.inventory.model.Supplier;
import com.smartinventory.inventory.repository.PurchaseOrderRepository;
import com.smartinventory.inventory.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;

    @Autowired
    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            SupplierRepository supplierRepository,
            InventoryService inventoryService) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Supplier createSupplier(Supplier supplier) {
        log.info("Creating supplier with code: {}", supplier.getCode());
        if (supplierRepository.existsByCode(supplier.getCode())) {
            throw new IllegalArgumentException("Supplier already exists with code: " + supplier.getCode());
        }
        return supplierRepository.save(supplier);
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierByCode(String code) {
        return supplierRepository.findByCode(code);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrder po) {
        log.info("Creating Purchase Order: {}", po.getOrderNumber());
        if (purchaseOrderRepository.existsByOrderNumber(po.getOrderNumber())) {
            throw new IllegalArgumentException("Purchase Order already exists: " + po.getOrderNumber());
        }
        po.setStatus(PurchaseOrder.STATUS_DRAFT);
        return purchaseOrderRepository.save(po);
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public Optional<PurchaseOrder> getPurchaseOrderByNumber(String orderNumber) {
        return purchaseOrderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public PurchaseOrder submitForApproval(String orderNumber) {
        log.info("Submitting Purchase Order {} for approval", orderNumber);
        PurchaseOrder po = purchaseOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + orderNumber));
        if (!PurchaseOrder.STATUS_DRAFT.equals(po.getStatus())) {
            throw new IllegalStateException("Only DRAFT purchase orders can be submitted for approval.");
        }
        po.setStatus(PurchaseOrder.STATUS_PENDING_APPROVAL);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder approvePurchaseOrder(String orderNumber) {
        log.info("Approving Purchase Order {}", orderNumber);
        PurchaseOrder po = purchaseOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + orderNumber));
        if (!PurchaseOrder.STATUS_PENDING_APPROVAL.equals(po.getStatus()) && !PurchaseOrder.STATUS_DRAFT.equals(po.getStatus())) {
            throw new IllegalStateException("Purchase order cannot be approved from status: " + po.getStatus());
        }
        po.setStatus(PurchaseOrder.STATUS_APPROVED);
        return purchaseOrderRepository.save(po);
    }

    @Transactional
    public PurchaseOrder receivePurchaseOrder(String orderNumber) {
        log.info("Receiving goods for Purchase Order {}", orderNumber);
        PurchaseOrder po = purchaseOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + orderNumber));

        if (!PurchaseOrder.STATUS_APPROVED.equals(po.getStatus())) {
            throw new IllegalStateException("Only APPROVED purchase orders can be received.");
        }

        // Adjust stock for each item received
        for (PurchaseOrderItem item : po.getItems()) {
            int qtyToReceive = item.getOrderedQuantity() - item.getReceivedQuantity();
            if (qtyToReceive > 0) {
                item.setReceivedQuantity(item.getOrderedQuantity());
                try {
                    inventoryService.adjustStock(item.getSku(), qtyToReceive, "PO_RECEIVING", po.getOrderNumber());
                } catch (IllegalArgumentException e) {
                    log.info("Creating new item for SKU {} during PO receiving", item.getSku());
                    com.smartinventory.inventory.model.InventoryItem newItem =
                            new com.smartinventory.inventory.model.InventoryItem(item.getSku(), qtyToReceive, 10, "MAIN_WH");
                    inventoryService.createInventoryItem(newItem);
                }
            }
        }

        po.setStatus(PurchaseOrder.STATUS_RECEIVED);
        return purchaseOrderRepository.save(po);
    }
}
