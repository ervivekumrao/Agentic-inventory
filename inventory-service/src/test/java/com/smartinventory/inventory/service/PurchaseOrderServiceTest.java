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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceTest {

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private InventoryService inventoryService;

    private PurchaseOrderService purchaseOrderService;

    @BeforeEach
    void setUp() {
        purchaseOrderService = new PurchaseOrderService(purchaseOrderRepository, supplierRepository, inventoryService);
    }

    @Test
    void createSupplier_shouldSave_whenNotExists() {
        Supplier supplier = new Supplier("SUP-001", "Acme Corp", "contact@acme.com", 5);
        when(supplierRepository.existsByCode("SUP-001")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        Supplier created = purchaseOrderService.createSupplier(supplier);
        assertNotNull(created);
        assertEquals("SUP-001", created.getCode());
    }

    @Test
    void createPurchaseOrder_shouldSaveAsDraft() {
        PurchaseOrder po = new PurchaseOrder("PO-1001", "SUP-001");
        po.addItem(new PurchaseOrderItem("SKU-001", 50, 12.5));
        when(purchaseOrderRepository.existsByOrderNumber("PO-1001")).thenReturn(false);
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder created = purchaseOrderService.createPurchaseOrder(po);
        assertNotNull(created);
        assertEquals("DRAFT", created.getStatus());
    }

    @Test
    void receivePurchaseOrder_shouldAdjustStockAndMarkReceived() {
        PurchaseOrder po = new PurchaseOrder("PO-1001", "SUP-001");
        po.setStatus(PurchaseOrder.STATUS_APPROVED);
        po.addItem(new PurchaseOrderItem("SKU-001", 50, 12.5));

        when(purchaseOrderRepository.findByOrderNumber("PO-1001")).thenReturn(Optional.of(po));
        when(purchaseOrderRepository.save(any(PurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        PurchaseOrder received = purchaseOrderService.receivePurchaseOrder("PO-1001");
        assertEquals("RECEIVED", received.getStatus());
        verify(inventoryService, times(1)).adjustStock("SKU-001", 50, "PO_RECEIVING", "PO-1001");
    }
}
