package com.smartinventory.inventory.db;

import com.smartinventory.inventory.db.dialect.QueryDialect;
import com.smartinventory.inventory.db.dialect.QueryDialectFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseQueryGeneratorTest {

    @Test
    void testDefaultDatabaseIsPostgres() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("");
        assertEquals(DatabaseType.POSTGRESQL, generator.getDatabaseType());
        assertTrue(generator.getPaginationClause(10, 0).contains("LIMIT 10 OFFSET 0"));
    }

    @Test
    void testPostgreSQLQueryGeneration() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("POSTGRESQL");
        assertEquals(DatabaseType.POSTGRESQL, generator.getDatabaseType());
        assertEquals(" LIMIT 10 OFFSET 5", generator.getPaginationClause(10, 5));
        assertTrue(generator.getTopStockItemsQuery(5).toUpperCase().contains("5"));
        assertNotNull(generator.getInventorySummaryQuery());
    }

    @Test
    void testMySQLQueryGeneration() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("mysql");
        assertEquals(DatabaseType.MYSQL, generator.getDatabaseType());
        assertEquals(" LIMIT 10 OFFSET 5", generator.getPaginationClause(10, 5));
        assertTrue(generator.getTopStockItemsQuery(5).toUpperCase().contains("5"));
    }

    @Test
    void testOracleQueryGeneration() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("ORACLE");
        assertEquals(DatabaseType.ORACLE, generator.getDatabaseType());
        assertEquals(" OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", generator.getPaginationClause(10, 5));
        assertTrue(generator.getTopStockItemsQuery(5).contains("FETCH FIRST 5 ROWS ONLY"));
    }

    @Test
    void testMssqlQueryGeneration() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("MSSQL");
        assertEquals(DatabaseType.MSSQL, generator.getDatabaseType());
        assertEquals(" OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", generator.getPaginationClause(10, 5));
        assertTrue(generator.getTopStockItemsQuery(5).contains("SELECT TOP (5)"));
    }

    @Test
    void testDb2QueryGeneration() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("DB2");
        assertEquals(DatabaseType.DB2, generator.getDatabaseType());
        assertEquals(" OFFSET 5 ROWS FETCH FIRST 10 ROWS ONLY", generator.getPaginationClause(10, 5));
        assertTrue(generator.getTopStockItemsQuery(5).contains("FETCH FIRST 5 ROWS ONLY"));
    }

    @Test
    void testDatabaseMetadata() {
        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("POSTGRESQL");
        Map<String, Object> metadata = generator.getDatabaseMetadata();
        assertEquals("POSTGRESQL", metadata.get("databaseType"));
        assertEquals("org.hibernate.dialect.PostgreSQLDialect", metadata.get("hibernateDialect"));
        assertNotNull(metadata.get("samplePaginationClause"));
        assertNotNull(metadata.get("sampleTopQuery"));
    }

    @Test
    void testDelegationToMockFactory() {
        QueryDialectFactory mockFactory = mock(QueryDialectFactory.class);
        QueryDialect mockDialect = mock(QueryDialect.class);

        when(mockFactory.getDialect(DatabaseType.POSTGRESQL)).thenReturn(mockDialect);
        when(mockDialect.executeCustomQuery("SELECT * FROM test", "arg1"))
                .thenReturn(List.of(Map.of("col", "val")));
        when(mockDialect.executeCustomUpdate("UPDATE test SET col = ?", "val"))
                .thenReturn(1);

        DatabaseQueryGenerator generator = new DatabaseQueryGenerator("POSTGRESQL", mockFactory);

        List<Map<String, Object>> result = generator.executeCustomQuery("SELECT * FROM test", "arg1");
        assertEquals(1, result.size());
        assertEquals("val", result.get(0).get("col"));

        int updated = generator.executeCustomUpdate("UPDATE test SET col = ?", "val");
        assertEquals(1, updated);
    }
}
