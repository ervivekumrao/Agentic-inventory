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
