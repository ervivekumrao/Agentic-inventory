package com.smartinventory.inventory.db.dialect;

import com.smartinventory.inventory.db.DatabaseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryDialectTest {

    private QueryDialectFactory factory;

    @BeforeEach
    void setUp() {
        factory = new QueryDialectFactory();
    }

    @Test
    void testPostgresDialect() {
        QueryDialect dialect = factory.getDialect(DatabaseType.POSTGRESQL);
        assertEquals(DatabaseType.POSTGRESQL, dialect.getDatabaseType());
        assertTrue(dialect.getPaginationClause(10, 0).contains("LIMIT 10 OFFSET 0"));
        assertTrue(dialect.getTopStockItemsQuery(5).toUpperCase().contains("5"));
        assertNotNull(dialect.getInventorySummaryQuery());
    }

    @Test
    void testMySqlDialect() {
        QueryDialect dialect = factory.getDialect(DatabaseType.MYSQL);
        assertEquals(DatabaseType.MYSQL, dialect.getDatabaseType());
        assertTrue(dialect.getPaginationClause(10, 5).contains("LIMIT 10 OFFSET 5"));
        assertTrue(dialect.getTopStockItemsQuery(5).toUpperCase().contains("5"));
    }

    @Test
    void testOracleDialect() {
        QueryDialect dialect = factory.getDialect(DatabaseType.ORACLE);
        assertEquals(DatabaseType.ORACLE, dialect.getDatabaseType());
        assertTrue(dialect.getPaginationClause(10, 5).contains("FETCH NEXT 10 ROWS ONLY"));
        assertTrue(dialect.getTopStockItemsQuery(5).contains("FETCH FIRST 5 ROWS ONLY"));
    }

    @Test
    void testSqlServerDialect() {
        QueryDialect dialect = factory.getDialect(DatabaseType.MSSQL);
        assertEquals(DatabaseType.MSSQL, dialect.getDatabaseType());
        assertTrue(dialect.getPaginationClause(10, 5).contains("FETCH NEXT 10 ROWS ONLY"));
        assertTrue(dialect.getTopStockItemsQuery(5).contains("SELECT TOP (5)"));
    }

    @Test
    void testDb2Dialect() {
        QueryDialect dialect = factory.getDialect(DatabaseType.DB2);
        assertEquals(DatabaseType.DB2, dialect.getDatabaseType());
        assertTrue(dialect.getPaginationClause(10, 5).contains("FETCH FIRST 10 ROWS ONLY"));
        assertTrue(dialect.getTopStockItemsQuery(5).contains("FETCH FIRST 5 ROWS ONLY"));
    }

    @Test
    void testDatabaseMetadata() {
        QueryDialect dialect = factory.getDialect(DatabaseType.POSTGRESQL);
        Map<String, Object> metadata = dialect.getDatabaseMetadata();
        assertEquals("POSTGRESQL", metadata.get("databaseType"));
        assertEquals(true, metadata.get("queryDslEngineActive"));
        assertEquals("Apache 2.0 (Permissive Open Source for all DBs)", metadata.get("licenseInfo"));
        assertNotNull(metadata.get("samplePaginationClause"));
        assertNotNull(metadata.get("sampleTopQuery"));
    }

    @Test
    void testCustomQueryWithoutJdbcTemplateReturnsEmpty() {
        QueryDialect dialect = factory.getDialect(DatabaseType.POSTGRESQL);
        assertTrue(dialect.executeCustomQuery("SELECT 1").isEmpty());
        assertEquals(0, dialect.executeCustomUpdate("UPDATE items SET qty = 1"));
    }

    @Test
    void testCustomQueryWithJdbcTemplate() {
        JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
        when(mockJdbcTemplate.queryForList(eq("SELECT * FROM items WHERE sku = ?"), eq("SKU-1")))
                .thenReturn(List.of(Map.of("sku", "SKU-1", "quantity", 100)));
        when(mockJdbcTemplate.update(eq("UPDATE items SET quantity = ? WHERE sku = ?"), eq(150), eq("SKU-1")))
                .thenReturn(1);

        PostgresQueryDialect postgresDialect = new PostgresQueryDialect(mockJdbcTemplate);

        List<Map<String, Object>> results = postgresDialect.executeCustomQuery("SELECT * FROM items WHERE sku = ?", "SKU-1");
        assertEquals(1, results.size());
        assertEquals("SKU-1", results.get(0).get("sku"));

        int rows = postgresDialect.executeCustomUpdate("UPDATE items SET quantity = ? WHERE sku = ?", 150, "SKU-1");
        assertEquals(1, rows);
    }

    @Test
    void testFactoryFallbackBehaviors() {
        QueryDialect dialectNull = factory.getDialect((DatabaseType) null);
        assertEquals(DatabaseType.POSTGRESQL, dialectNull.getDatabaseType());

        QueryDialect dialectStr = factory.getDialect("UNKNOWN_DB");
        assertEquals(DatabaseType.POSTGRESQL, dialectStr.getDatabaseType());
    }

    @Test
    void testDatabaseTypeFromString() {
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString(null));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("   "));
        assertEquals(DatabaseType.POSTGRESQL, DatabaseType.fromString("INVALID_DB"));
        assertEquals(DatabaseType.MYSQL, DatabaseType.fromString("mysql"));
        assertEquals(DatabaseType.ORACLE, DatabaseType.fromString("ORACLE"));
        assertEquals(DatabaseType.MSSQL, DatabaseType.fromString("mssql"));
        assertEquals(DatabaseType.DB2, DatabaseType.fromString("db2"));
    }
}
