package com.smartinventory.inventory.repository;

import com.smartinventory.inventory.model.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    List<StockTransaction> findBySku(String sku);
}
