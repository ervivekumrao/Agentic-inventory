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

import com.smartinventory.inventory.model.StockPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockPositionRepository extends JpaRepository<StockPosition, Long> {

    List<StockPosition> findBySku(String sku);

    Optional<StockPosition> findBySkuAndLocationIdAndLotId(String sku, String locationId, String lotId);

    @Modifying
    @Query("UPDATE StockPosition p SET p.reservedQuantity = p.reservedQuantity + :qty " +
           "WHERE p.sku = :sku AND p.locationId = :locationId AND p.lotId = :lotId " +
           "AND (p.onHandQuantity - p.reservedQuantity - p.safetyStockQuantity) >= :qty")
    int reserveStock(@Param("sku") String sku, @Param("locationId") String locationId, @Param("lotId") String lotId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE StockPosition p SET p.onHandQuantity = p.onHandQuantity - :qty, p.reservedQuantity = p.reservedQuantity - :qty " +
           "WHERE p.sku = :sku AND p.locationId = :locationId AND p.lotId = :lotId AND p.reservedQuantity >= :qty")
    int claimStock(@Param("sku") String sku, @Param("locationId") String locationId, @Param("lotId") String lotId, @Param("qty") int qty);
}
