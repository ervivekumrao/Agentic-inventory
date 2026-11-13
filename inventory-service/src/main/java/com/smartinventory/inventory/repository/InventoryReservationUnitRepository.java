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

import com.smartinventory.inventory.model.InventoryReservationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InventoryReservationUnitRepository extends JpaRepository<InventoryReservationUnit, Long> {

    /**
     * Shopify-style non-blocking reservation selection using FOR UPDATE SKIP LOCKED.
     * Skips currently locked units by other concurrent checkouts and locks the next available units immediately.
     */
    @Query(value = "SELECT * FROM inventory_reservation_units WHERE sku = :sku AND status = 'AVAILABLE' LIMIT :count FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<InventoryReservationUnit> fetchAvailableUnitsForUpdate(@Param("sku") String sku, @Param("count") int count);

    List<InventoryReservationUnit> findByReservationId(String reservationId);

    long countBySkuAndStatus(String sku, String status);

    List<InventoryReservationUnit> findByStatusAndExpiresAtBefore(String status, Instant threshold);

    List<InventoryReservationUnit> findByStatusAndCreatedAtBefore(String status, Instant threshold);

    void deleteBySku(String sku);

    long countBySku(String sku);
}
