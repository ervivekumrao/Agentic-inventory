package com.smartinventory.inventory.db.dialect;

import com.smartinventory.inventory.db.DatabaseType;

import java.util.List;
import java.util.Map;

/**
 * Interface defining SQL dialect syntax rules, custom query execution, and database metadata.
 */
public interface QueryDialect {

    /**
     * Returns the active database type associated with this dialect.
     */
    DatabaseType getDatabaseType();

    /**
     * Generates DB-specific pagination SQL clause.
     */
    String getPaginationClause(int limit, int offset);

    /**
     * Generates DB-specific query to retrieve top N inventory items sorted by quantity descending.
     */
    String getTopStockItemsQuery(int limit);

    /**
     * Generates DB-specific inventory valuation summary query.
     */
    String getInventorySummaryQuery();

    /**
     * Returns database metadata and query generation info.
     */
    Map<String, Object> getDatabaseMetadata();

    /**
     * Executes custom parameterized SELECT query and returns results as maps.
     */
    List<Map<String, Object>> executeCustomQuery(String sql, Object... params);

    /**
     * Executes custom parameterized UPDATE / INSERT / DELETE query and returns affected row count.
     */
    int executeCustomUpdate(String sql, Object... params);
}
