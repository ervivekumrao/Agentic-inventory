package com.smartinventory.inventory.db.dialect;

import com.querydsl.sql.SQLServer2012Templates;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SqlServerQueryDialect extends AbstractQueryDialect {

    @Autowired
    public SqlServerQueryDialect(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.MSSQL;
    }

    @Override
    public SQLTemplates getQueryDslTemplates() {
        return SQLServer2012Templates.builder().build();
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        return String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        return "SELECT TOP (" + limit + ") sku, quantity, warehouse_location FROM inventory_items ORDER BY quantity DESC";
    }
}
