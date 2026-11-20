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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class BulkDataImporterTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private BulkDataImporter bulkDataImporter;

    @BeforeEach
    void setUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeBatch()).thenReturn(new int[]{1, 1});

        bulkDataImporter = new BulkDataImporter(dataSource);
    }

    @Test
    @DisplayName("Should successfully bulk import items from CSV stream via connection pool batching")
    void importFromCsvStream_Success() {
        String csvContent = """
                sku,quantity,reorder_threshold,warehouse_location
                SKU-TEST-001,50,10,Warehouse-North
                SKU-TEST-002,30,5,Central-Hub
                """;

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        BulkImportResult result = bulkDataImporter.importFromCsvStream(stream);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRecords());
        assertEquals(2, result.getImportedRecords());
        assertEquals(0, result.getFailedRecords());
        assertTrue(result.getDurationMs() >= 0);
    }

    @Test
    @DisplayName("Should successfully bulk import from direct InventoryItem list")
    void importItemList_Success() throws Exception {
        List<InventoryItem> items = List.of(
                new InventoryItem("ITEM-001", 100, 15, "Warehouse-East"),
                new InventoryItem("ITEM-002", 200, 20, "Warehouse-West")
        );

        BulkImportResult result = bulkDataImporter.importItemList(items);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRecords());
        assertEquals(2, result.getImportedRecords());
        verify(preparedStatement, times(2)).addBatch();
        verify(connection, times(1)).commit();
    }

    @Test
    @DisplayName("Should return error result if CSV header is missing required 'sku' column")
    void importFromCsvStream_MissingSkuHeader() {
        String invalidCsv = """
                quantity,warehouse_location
                50,Warehouse-North
                """;

        InputStream stream = new ByteArrayInputStream(invalidCsv.getBytes(StandardCharsets.UTF_8));
        BulkImportResult result = bulkDataImporter.importFromCsvStream(stream);

        assertFalse(result.isSuccess());
        assertFalse(result.getErrors().isEmpty());
    }
}
