package com.smartinventory.inventory.db.dialect;

import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OracleQueryDialect extends AbstractQueryDialect {

    @Autowired
    public OracleQueryDialect(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
    }

    @Override
    public SQLTemplates getQueryDslTemplates() {
        return OracleTemplates.builder().build();
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        return String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        return "SELECT sku, quantity, warehouse_location FROM inventory_items ORDER BY quantity DESC FETCH FIRST " + limit + " ROWS ONLY";
    }
}
