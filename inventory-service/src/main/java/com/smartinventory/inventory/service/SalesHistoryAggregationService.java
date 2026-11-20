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

import com.smartinventory.inventory.model.StockTransaction;
import com.smartinventory.inventory.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service aggregating historical sales transactions from the immutable StockTransaction ledger
 * into structured sales time-series data for the AI Forecasting Service.
 */
@Service
public class SalesHistoryAggregationService {

    private final StockTransactionRepository transactionRepository;

    @Autowired
    public SalesHistoryAggregationService(StockTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Aggregates daily sales quantity for a SKU over the past specified number of days.
     */
    @Transactional(readOnly = true)
    public SalesHistoryDto getSalesHistoryForSku(String sku, int daysBack) {
        int days = Math.max(1, Math.min(daysBack, 365));
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<StockTransaction> transactions = transactionRepository
                .findBySkuAndTimestampAfterOrderByTimestampAsc(sku, since);

        // Group sales deductions by LocalDate
        Map<LocalDate, Integer> dailySales = transactions.stream()
                .filter(tx -> (tx.getQuantityChange() != null && tx.getQuantityChange() < 0) || "RESERVATION_CLAIM".equals(tx.getTransactionType()))
                .collect(Collectors.groupingBy(
                        tx -> LocalDate.ofInstant(tx.getTimestamp(), ZoneId.systemDefault()),
                        Collectors.summingInt(tx -> Math.abs(tx.getQuantityChange() != null ? tx.getQuantityChange() : 0))
                ));

        List<DailySalesPoint> salesPoints = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int qty = dailySales.getOrDefault(date, 0);
            salesPoints.add(new DailySalesPoint(date.toString(), qty));
        }

        long totalSalesUnits = salesPoints.stream().mapToLong(DailySalesPoint::unitsSold).sum();
        boolean sufficientData = transactions.size() >= 5;

        return new SalesHistoryDto(
                sku,
                days,
                transactions.size(),
                totalSalesUnits,
                sufficientData,
                salesPoints
        );
    }

    public record DailySalesPoint(String date, int unitsSold) {}

    public record SalesHistoryDto(
            String sku,
            int periodDays,
            int transactionCount,
            long totalUnitsSold,
            boolean hasSufficientData,
            List<DailySalesPoint> dailyHistory
    ) {}
}
