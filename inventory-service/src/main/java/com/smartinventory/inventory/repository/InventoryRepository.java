package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);
    List<InventoryItem> findByQuantityLessThanEqual(Integer threshold);
    boolean existsBySku(String sku);
}
