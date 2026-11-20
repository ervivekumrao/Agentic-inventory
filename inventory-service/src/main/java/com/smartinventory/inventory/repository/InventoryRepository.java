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

package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.model.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.sku = :sku")
    Optional<InventoryItem> findBySkuForUpdate(@Param("sku") String sku);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE InventoryItem i SET i.quantity = i.quantity - :count WHERE i.sku = :sku AND i.quantity >= :count")
    int decrementStockIfSufficient(@Param("sku") String sku, @Param("count") int count);

    @Query("SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderThreshold")
    List<InventoryItem> findLowStockItems();

    List<InventoryItem> findTop250By();

    boolean existsBySku(String sku);

    void deleteBySku(String sku);

    @Query("SELECT COUNT(i) AS totalSkus, COALESCE(SUM(i.quantity), 0) AS totalQuantity, " +
           "SUM(CASE WHEN i.quantity <= i.reorderThreshold THEN 1 ELSE 0 END) AS lowStockCount, " +
           "SUM(CASE WHEN i.quantity = 0 THEN 1 ELSE 0 END) AS outOfStockCount, " +
           "SUM(CASE WHEN i.quantity > (i.reorderThreshold * 5) AND i.quantity >= 50 THEN 1 ELSE 0 END) AS overstockedCount " +
           "FROM InventoryItem i")
    InventorySummaryProjection getPortfolioSummaryProjection();

    @Query("SELECT i.warehouseLocation AS warehouseLocation, COUNT(i) AS totalSkus, COALESCE(SUM(i.quantity), 0) AS totalQuantity " +
           "FROM InventoryItem i GROUP BY i.warehouseLocation")
    List<WarehouseSummaryProjection> getWarehouseSummaryProjections();
}
