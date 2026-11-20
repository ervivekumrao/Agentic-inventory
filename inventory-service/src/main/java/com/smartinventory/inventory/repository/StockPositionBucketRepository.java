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

import com.smartinventory.inventory.model.StockPositionBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockPositionBucketRepository extends JpaRepository<StockPositionBucket, Long> {

    List<StockPositionBucket> findBySkuAndLocationId(String sku, String locationId);

    Optional<StockPositionBucket> findBySkuAndLocationIdAndBucketId(String sku, String locationId, int bucketId);

    @Modifying
    @Query("UPDATE StockPositionBucket b SET b.reservedQuantity = b.reservedQuantity + :qty " +
           "WHERE b.sku = :sku AND b.locationId = :locationId AND b.bucketId = :bucketId " +
           "AND (b.onHandQuantity - b.reservedQuantity - b.safetyStockQuantity) >= :qty")
    int reserveFromBucket(@Param("sku") String sku, @Param("locationId") String locationId, @Param("bucketId") int bucketId, @Param("qty") int qty);

    @Modifying
    @Query("UPDATE StockPositionBucket b SET b.onHandQuantity = b.onHandQuantity - :qty, b.reservedQuantity = b.reservedQuantity - :qty " +
           "WHERE b.sku = :sku AND b.locationId = :locationId AND b.bucketId = :bucketId AND b.reservedQuantity >= :qty")
    int claimFromBucket(@Param("sku") String sku, @Param("locationId") String locationId, @Param("bucketId") int bucketId, @Param("qty") int qty);
}
