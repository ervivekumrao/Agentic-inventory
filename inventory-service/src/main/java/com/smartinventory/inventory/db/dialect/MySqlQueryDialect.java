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
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
import com.smartinventory.inventory.security.sqli.SqlSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MySqlQueryDialect extends AbstractQueryDialect {

    @Autowired
    public MySqlQueryDialect(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public SQLTemplates getQueryDslTemplates() {
        return MySQLTemplates.builder().build();
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        int safeLimit = SqlSanitizer.validateLimit(limit);
        int safeOffset = SqlSanitizer.validateOffset(offset);
        return String.format(" LIMIT %d OFFSET %d", safeLimit, safeOffset);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        int safeLimit = SqlSanitizer.validateLimit(limit);
        Configuration configuration = new Configuration(getQueryDslTemplates());
        SQLQuery<Tuple> query = new SQLQuery<>(configuration);

        StringPath sku = Expressions.stringPath("sku");
        NumberPath<Integer> quantity = Expressions.numberPath(Integer.class, "quantity");
        StringPath warehouseLocation = Expressions.stringPath("warehouse_location");
        PathBuilder<Object> inventoryItems = new PathBuilder<>(Object.class, "inventory_items");

        String sql = query.select(sku, quantity, warehouseLocation)
                .from(inventoryItems)
                .orderBy(quantity.desc())
                .limit(safeLimit)
                .getSQL().getSQL();

        return sql.replace("?", String.valueOf(safeLimit));
    }
}
