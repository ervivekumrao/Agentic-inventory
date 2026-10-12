package com.smartinventory.inventory.db.dialect;

import com.querydsl.sql.DB2Templates;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
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
        return String.format(" OFFSET %d ROWS FETCH FIRST %d ROWS ONLY", offset, limit);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        return "SELECT sku, quantity, warehouse_location FROM inventory_items ORDER BY quantity DESC FETCH FIRST " + limit + " ROWS ONLY";
    }
}
