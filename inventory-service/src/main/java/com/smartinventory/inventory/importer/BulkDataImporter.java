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

package com.smartinventory.inventory.importer;

import com.smartinventory.inventory.dto.BulkImportResult;
import com.smartinventory.inventory.model.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Technology-agnostic, high-performance bulk data importer utilizing connection pooling
 * and batch PreparedStatements for enterprise initial inventory population.
 */
@Service
public class BulkDataImporter {

    private static final Logger log = LoggerFactory.getLogger(BulkDataImporter.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final DataSource dataSource;

    public BulkDataImporter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Imports inventory items from CSV content using connection pooling and JDBC batching.
     *
     * @param inputStream CSV input stream
     * @return BulkImportResult summarizing processed count, errors, and execution time
     */
    public BulkImportResult importFromCsvStream(InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        BulkImportResult result = new BulkImportResult();

        List<InventoryItem> items = parseCsvStream(inputStream, result);
        if (items.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("No valid inventory items parsed from CSV input.");
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }

        return executeBatchImport(items, startTime, result);
    }

    /**
     * Imports inventory items from raw CSV string content.
     */
    public BulkImportResult importFromCsvContent(String csvContent) {
        InputStream stream = new java.io.ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        return importFromCsvStream(stream);
    }

    /**
     * Bulk imports a collection of InventoryItem objects into the database via JDBC batching.
     */
    public BulkImportResult importItemList(List<InventoryItem> items) {
        long startTime = System.currentTimeMillis();
        BulkImportResult result = new BulkImportResult();
        return executeBatchImport(items, startTime, result);
    }

    private BulkImportResult executeBatchImport(List<InventoryItem> items, long startTime, BulkImportResult result) {
        result.setTotalRecords(items.size());

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            log.info("🚀 Connection pool link acquired for bulk import. Target Database: {}, Total Records: {}", dbProductName, items.size());

            String sql = buildUpsertSql(dbProductName);

            int totalImported = 0;
            int totalFailed = 0;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                int count = 0;
                for (InventoryItem item : items) {
                    pstmt.setString(1, item.getSku());
                    pstmt.setInt(2, item.getQuantity() != null ? item.getQuantity() : 0);
                    pstmt.setInt(3, item.getReorderThreshold() != null ? item.getReorderThreshold() : 10);
                    pstmt.setString(4, item.getWarehouseLocation() != null ? item.getWarehouseLocation() : "Central-Hub");

                    pstmt.addBatch();
                    count++;

                    if (count % DEFAULT_BATCH_SIZE == 0) {
                        int[] batchResults = pstmt.executeBatch();
                        connection.commit();
                        totalImported += countBatchSuccesses(batchResults);
                        log.debug("Committed batch of {} items. Cumulative total: {}", count, totalImported);
                    }
                }

                if (count % DEFAULT_BATCH_SIZE != 0) {
                    int[] batchResults = pstmt.executeBatch();
                    connection.commit();
                    totalImported += countBatchSuccesses(batchResults);
                }
            } catch (SQLException e) {
                log.error("❌ Error executing batch insert. Rolling back transaction: {}", e.getMessage(), e);
                connection.rollback();
                totalFailed = items.size() - totalImported;
                result.addError("Batch transaction failed and rolled back: " + e.getMessage());
            }

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setImportedRecords(totalImported);
            result.setFailedRecords(totalFailed);
            result.setSuccess(totalFailed == 0);
            result.setMessage(String.format("Successfully bulk imported %d of %d items in %d ms",
                    totalImported, items.size(), duration));

            log.info("✅ Bulk Import finished: {} imported, {} failed in {} ms", totalImported, totalFailed, duration);
            return result;

        } catch (SQLException e) {
            log.error("❌ Database Connection Error during bulk import", e);
            result.setSuccess(false);
            result.setMessage("Database connection error: " + e.getMessage());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    private String buildUpsertSql(String dbProductName) {
        String dbNameLower = dbProductName != null ? dbProductName.toLowerCase() : "";
        if (dbNameLower.contains("mysql") || dbNameLower.contains("mariadb")) {
            return "INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, version) " +
                    "VALUES (?, ?, ?, ?, 0) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "quantity = VALUES(quantity), " +
                    "reorder_threshold = VALUES(reorder_threshold), " +
                    "warehouse_location = VALUES(warehouse_location), " +
                    "version = inventory_items.version + 1";
        }
        // PostgreSQL, H2, Oracle 23c+, and ANSI standard ON CONFLICT
        return "INSERT INTO inventory_items (sku, quantity, reorder_threshold, warehouse_location, version) " +
                "VALUES (?, ?, ?, ?, 0) " +
                "ON CONFLICT (sku) DO UPDATE SET " +
                "quantity = EXCLUDED.quantity, " +
                "reorder_threshold = EXCLUDED.reorder_threshold, " +
                "warehouse_location = EXCLUDED.warehouse_location, " +
                "version = inventory_items.version + 1";
    }

    private List<InventoryItem> parseCsvStream(InputStream inputStream, BulkImportResult result) {
        List<InventoryItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return items;
            }

            String[] headers = headerLine.split(",");
            int skuIdx = -1, qtyIdx = -1, thresholdIdx = -1, warehouseIdx = -1;

            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.equalsIgnoreCase("sku")) skuIdx = i;
                else if (h.equalsIgnoreCase("quantity") || h.equalsIgnoreCase("qty")) qtyIdx = i;
                else if (h.equalsIgnoreCase("reorder_threshold") || h.equalsIgnoreCase("reorderthreshold") || h.equalsIgnoreCase("threshold")) thresholdIdx = i;
                else if (h.equalsIgnoreCase("warehouse_location") || h.equalsIgnoreCase("warehouselocation") || h.equalsIgnoreCase("warehouse")) warehouseIdx = i;
            }

            if (skuIdx == -1) {
                result.addError("CSV Header missing required 'sku' column.");
                return items;
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length <= skuIdx) {
                    result.addError("Line " + lineNumber + ": Insufficient columns");
                    continue;
                }

                String sku = tokens[skuIdx].trim();
                if (sku.isEmpty()) {
                    result.addError("Line " + lineNumber + ": SKU cannot be empty");
                    continue;
                }

                int qty = (qtyIdx != -1 && tokens.length > qtyIdx && !tokens[qtyIdx].trim().isEmpty())
                        ? Integer.parseInt(tokens[qtyIdx].trim()) : 0;
                int threshold = (thresholdIdx != -1 && tokens.length > thresholdIdx && !tokens[thresholdIdx].trim().isEmpty())
                        ? Integer.parseInt(tokens[thresholdIdx].trim()) : 10;
                String warehouse = (warehouseIdx != -1 && tokens.length > warehouseIdx && !tokens[warehouseIdx].trim().isEmpty())
                        ? tokens[warehouseIdx].trim() : "Central-Hub";

                items.add(new InventoryItem(sku, qty, threshold, warehouse));
            }

        } catch (Exception e) {
            log.error("Error parsing CSV stream", e);
            result.addError("CSV Parse Error: " + e.getMessage());
        }

        return items;
    }

    private int countBatchSuccesses(int[] batchResults) {
        int count = 0;
        for (int res : batchResults) {
            if (res >= 0 || res == Statement.SUCCESS_NO_INFO) {
                count++;
            }
        }
        return count;
    }
}
