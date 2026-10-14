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

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.security.sqli.SqlInjectionDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for QueryDSL and Spring JDBC powered database query dialects.
 */
public abstract class AbstractQueryDialect implements QueryDialect {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final JdbcTemplate jdbcTemplate;

    protected AbstractQueryDialect(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the QueryDSL SQLTemplates for the specific database dialect.
     */
    public abstract SQLTemplates getQueryDslTemplates();

    @Override
    public String getInventorySummaryQuery() {
        SQLTemplates templates = getQueryDslTemplates();
        Configuration configuration = new Configuration(templates);
        SQLQuery<Tuple> query = new SQLQuery<>(configuration);

        StringPath warehouseLocation = Expressions.stringPath("warehouse_location");
        NumberPath<Integer> quantity = Expressions.numberPath(Integer.class, "quantity");
        PathBuilder<Object> inventoryItems = new PathBuilder<>(Object.class, "inventory_items");

        return query.select(
                warehouseLocation,
                Expressions.numberTemplate(Long.class, "count(*)").as("sku_count"),
                quantity.sum().as("total_quantity")
        ).from(inventoryItems)
        .groupBy(warehouseLocation)
        .getSQL().getSQL();
    }

    @Override
    public Map<String, Object> getDatabaseMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("databaseType", getDatabaseType().name());
        metadata.put("hibernateDialect", getDatabaseType().getHibernateDialect());
        metadata.put("defaultUrl", getDatabaseType().getDefaultUrl());
        metadata.put("samplePaginationClause", getPaginationClause(10, 0));
        metadata.put("sampleTopQuery", getTopStockItemsQuery(5));
        metadata.put("queryDslEngineActive", true);
        metadata.put("licenseInfo", "Apache 2.0 (Permissive Open Source for all DBs)");
        return metadata;
    }

    @Override
    public List<Map<String, Object>> executeCustomQuery(String sql, Object... params) {
        SqlInjectionDetector.validateQuery(sql);
        if (jdbcTemplate == null) {
            log.warn("Cannot execute custom query; JdbcTemplate is not configured.");
            return Collections.emptyList();
        }
        log.info("Executing custom QueryDSL/Spring-JDBC query: {}", sql);
        return jdbcTemplate.queryForList(sql, params);
    }

    @Override
    public int executeCustomUpdate(String sql, Object... params) {
        SqlInjectionDetector.validateQuery(sql);
        if (jdbcTemplate == null) {
            log.warn("Cannot execute custom update; JdbcTemplate is not configured.");
            return 0;
        }
        log.info("Executing custom QueryDSL/Spring-JDBC update: {}", sql);
        return jdbcTemplate.update(sql, params);
    }
}
