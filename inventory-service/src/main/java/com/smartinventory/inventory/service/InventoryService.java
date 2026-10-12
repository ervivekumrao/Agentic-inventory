package com.smartinventory.inventory.service;

import com.smartinventory.inventory.kafka.KafkaProducerService;
import com.smartinventory.inventory.kafka.StockUpdateEvent;
import com.smartinventory.inventory.model.InventoryItem;
import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.repository.InventoryRepository;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    public InventoryService(InventoryRepository inventoryRepository,
                            StockTransactionRepository transactionRepository,
                            KafkaProducerService kafkaProducerService) {
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<InventoryItem> getAllInventory() {
        log.trace("Fetching all inventory items from repository");
        return inventoryRepository.findAll();
    }

    public Optional<InventoryItem> getInventoryBySku(String sku) {
        log.trace("Fetching inventory item by SKU: {}", sku);
        return inventoryRepository.findBySku(sku);
    }

    public List<InventoryItem> getLowStockItems() {
        log.trace("Evaluating low stock items against reorder thresholds");
        List<InventoryItem> lowStockItems = inventoryRepository.findAll().stream()
                .filter(item -> item.getQuantity() <= item.getReorderThreshold())
                .toList();
        log.debug("Identified {} low stock items", lowStockItems.size());
        return lowStockItems;
    }

    @Transactional
    public InventoryItem createInventoryItem(InventoryItem item) {
        log.debug("Creating inventory item for SKU: {}", item.getSku());
        if (inventoryRepository.existsBySku(item.getSku())) {
            log.warn("Attempted to create duplicate inventory item for SKU: {}", item.getSku());
            throw new IllegalArgumentException("Inventory item for SKU " + item.getSku() + " already exists.");
        }
        InventoryItem saved = inventoryRepository.save(item);
        transactionRepository.save(new StockTransaction(saved.getSku(), saved.getQuantity(), "INITIAL", "SETUP"));
        kafkaProducerService.sendStockUpdateEvent(new StockUpdateEvent(saved.getSku(), saved.getQuantity(), "CREATED"));
        log.debug("Inventory item saved and event published for SKU: {}", saved.getSku());
        return saved;
    }

    @Transactional
    public List<InventoryItem> createInventoryItemsBulk(List<InventoryItem> items) {
        log.debug("Bulk creating {} inventory items", items.size());
        List<InventoryItem> savedItems = inventoryRepository.saveAll(items);
        for (InventoryItem saved : savedItems) {
            transactionRepository.save(new StockTransaction(saved.getSku(), saved.getQuantity(), "INITIAL", "SETUP"));
            try {
                kafkaProducerService.sendStockUpdateEvent(new StockUpdateEvent(saved.getSku(), saved.getQuantity(), "CREATED"));
            } catch (Exception e) {
                log.warn("Failed to send Kafka event for bulk item {}: {}", saved.getSku(), e.getMessage());
            }
        }
        log.debug("Successfully bulk-saved {} inventory items", savedItems.size());
        return savedItems;
    }

    @Transactional
    public InventoryItem adjustStock(String sku, int quantityChange, String transactionType, String referenceId) {
        log.debug("Adjusting stock for SKU: {} by {}", sku, quantityChange);
        InventoryItem item = inventoryRepository.findBySku(sku)
                .orElseThrow(() -> {
                    log.warn("Stock adjustment failed, SKU not found: {}", sku);
                    return new IllegalArgumentException("Inventory item not found for SKU: " + sku);
                });

        int newQuantity = item.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            log.warn("Insufficient stock for SKU: {}. Current: {}, Requested change: {}", sku, item.getQuantity(), quantityChange);
            throw new IllegalArgumentException("Insufficient stock for SKU: " + sku + ". Current: " + item.getQuantity() + ", Requested change: " + quantityChange);
        }

        item.setQuantity(newQuantity);
        InventoryItem updated = inventoryRepository.save(item);

        transactionRepository.save(new StockTransaction(sku, quantityChange, transactionType, referenceId));
        kafkaProducerService.sendStockUpdateEvent(new StockUpdateEvent(sku, newQuantity, transactionType));

        log.debug("Updated stock for SKU: {} to new quantity: {}", sku, newQuantity);
        return updated;
    }

    public List<StockTransaction> getTransactionsForSku(String sku) {
        log.trace("Fetching transaction history for SKU: {}", sku);
        return transactionRepository.findBySku(sku);
    }
}
