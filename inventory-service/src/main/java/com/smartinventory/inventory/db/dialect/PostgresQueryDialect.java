package com.smartinventory.inventory.db.dialect;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.PostgreSQLTemplates;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLTemplates;
import com.smartinventory.inventory.db.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresQueryDialect extends AbstractQueryDialect {

    @Autowired
    public PostgresQueryDialect(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public SQLTemplates getQueryDslTemplates() {
        return PostgreSQLTemplates.builder().build();
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        return String.format(" LIMIT %d OFFSET %d", limit, offset);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        Configuration configuration = new Configuration(getQueryDslTemplates());
        SQLQuery<Tuple> query = new SQLQuery<>(configuration);

        StringPath sku = Expressions.stringPath("sku");
        NumberPath<Integer> quantity = Expressions.numberPath(Integer.class, "quantity");
        StringPath warehouseLocation = Expressions.stringPath("warehouse_location");
        PathBuilder<Object> inventoryItems = new PathBuilder<>(Object.class, "inventory_items");

        String sql = query.select(sku, quantity, warehouseLocation)
                .from(inventoryItems)
                .orderBy(quantity.desc())
                .limit(limit)
                .getSQL().getSQL();

        return sql.replace("?", String.valueOf(limit));
    }
}
