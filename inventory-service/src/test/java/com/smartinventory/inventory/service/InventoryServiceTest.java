package com.smartinventory.inventory.service;

import com.smartinventory.inventory.kafka.KafkaProducerService;
import com.smartinventory.inventory.kafka.StockUpdateEvent;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = new InventoryItem("SKU-001", 50, 10, "Warehouse-A");
        item.setId(1L);
    }

    @Test
    void getAllInventory_shouldReturnList() {
        when(inventoryRepository.findAll()).thenReturn(List.of(item));
        List<InventoryItem> items = inventoryService.getAllInventory();
        assertEquals(1, items.size());
    }

    @Test
    void getInventoryBySku_shouldReturnItem_whenExists() {
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.of(item));
        Optional<InventoryItem> found = inventoryService.getInventoryBySku("SKU-001");
        assertTrue(found.isPresent());
        assertEquals(50, found.get().getQuantity());
    }

    @Test
    void getLowStockItems_shouldReturnFilteredList() {
        InventoryItem lowItem = new InventoryItem("SKU-002", 5, 10, "Warehouse-A");
        when(inventoryRepository.findAll()).thenReturn(List.of(item, lowItem));

        List<InventoryItem> lowStock = inventoryService.getLowStockItems();
        assertEquals(1, lowStock.size());
        assertEquals("SKU-002", lowStock.get(0).getSku());
    }

    @Test
    void createInventoryItem_shouldSaveAndPublish_whenNotExists() {
        when(inventoryRepository.existsBySku("SKU-001")).thenReturn(false);
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(item);

        InventoryItem created = inventoryService.createInventoryItem(item);
        assertNotNull(created);
        verify(transactionRepository, times(1)).save(any(StockTransaction.class));
        verify(kafkaProducerService, times(1)).sendStockUpdateEvent(any(StockUpdateEvent.class));
    }

    @Test
    void createInventoryItem_shouldThrowException_whenExists() {
        when(inventoryRepository.existsBySku("SKU-001")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> inventoryService.createInventoryItem(item));
    }

    @Test
    void adjustStock_shouldUpdate_whenValid() {
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.of(item));
        when(inventoryRepository.save(any(InventoryItem.class))).thenReturn(item);

        InventoryItem updated = inventoryService.adjustStock("SKU-001", 10, "IN", "PO-123");
        assertEquals(60, updated.getQuantity());
        verify(transactionRepository, times(1)).save(any(StockTransaction.class));
        verify(kafkaProducerService, times(1)).sendStockUpdateEvent(any(StockUpdateEvent.class));
    }

    @Test
    void adjustStock_shouldThrowException_whenInsufficientStock() {
        when(inventoryRepository.findBySku("SKU-001")).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> inventoryService.adjustStock("SKU-001", -100, "OUT", "ORDER-1"));
    }

    @Test
    void adjustStock_shouldThrowException_whenItemNotFound() {
        when(inventoryRepository.findBySku("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> inventoryService.adjustStock("UNKNOWN", 10, "IN", "PO-1"));
    }

    @Test
    void getTransactionsForSku_shouldReturnList() {
        StockTransaction tx = new StockTransaction("SKU-001", 50, "INITIAL", "SETUP");
        when(transactionRepository.findBySku("SKU-001")).thenReturn(List.of(tx));

        List<StockTransaction> txs = inventoryService.getTransactionsForSku("SKU-001");
        assertEquals(1, txs.size());
    }
}
