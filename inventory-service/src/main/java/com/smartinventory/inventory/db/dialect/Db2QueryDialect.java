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

import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
import com.smartinventory.inventory.security.sqli.SqlSanitizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Db2QueryDialect extends AbstractQueryDialect {

    @Autowired
    public Db2QueryDialect(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.DB2;
    }

    @Override
    public SQLTemplates getQueryDslTemplates() {
        return DB2Templates.builder().build();
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        int safeLimit = SqlSanitizer.validateLimit(limit);
        int safeOffset = SqlSanitizer.validateOffset(offset);
        return String.format(" OFFSET %d ROWS FETCH FIRST %d ROWS ONLY", safeOffset, safeLimit);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        int safeLimit = SqlSanitizer.validateLimit(limit);
        return "SELECT sku, quantity, warehouse_location FROM inventory_items ORDER BY quantity DESC FETCH FIRST " + safeLimit + " ROWS ONLY";
    }
}
