package com.smartinventory.inventory.db;

import com.smartinventory.inventory.db.dialect.QueryDialect;
import com.smartinventory.inventory.db.dialect.QueryDialectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Facade component implementing {@link QueryDialect} and delegating query generation
 * to polymorphic database dialects via {@link QueryDialectFactory}.
 */
@Service
@Primary
public class DatabaseQueryGenerator implements QueryDialect {

    private static final Logger log = LoggerFactory.getLogger(DatabaseQueryGenerator.class);

    private final DatabaseType databaseType;
    private final QueryDialectFactory dialectFactory;

    @Autowired
    public DatabaseQueryGenerator(
            @Value("${inventory.database.type:POSTGRESQL}") String dbTypeStr,
            QueryDialectFactory dialectFactory) {
        this.databaseType = DatabaseType.fromString(dbTypeStr);
        this.dialectFactory = dialectFactory;
        log.info("Initialized DatabaseQueryGenerator for DB type: {} delegating to QueryDialectFactory", this.databaseType);
    }

    public DatabaseQueryGenerator(String dbTypeStr) {
        this(dbTypeStr, new QueryDialectFactory());
    }

    private QueryDialect activeDialect() {
        return dialectFactory.getDialect(databaseType);
    }

    @Override
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    @Override
    public String getPaginationClause(int limit, int offset) {
        return activeDialect().getPaginationClause(limit, offset);
    }

    @Override
    public String getTopStockItemsQuery(int limit) {
        return activeDialect().getTopStockItemsQuery(limit);
    }

    @Override
    public String getInventorySummaryQuery() {
        return activeDialect().getInventorySummaryQuery();
    }

    @Override
    public Map<String, Object> getDatabaseMetadata() {
        return activeDialect().getDatabaseMetadata();
    }

    @Override
    public List<Map<String, Object>> executeCustomQuery(String sql, Object... params) {
        return activeDialect().executeCustomQuery(sql, params);
    }

    @Override
    public int executeCustomUpdate(String sql, Object... params) {
        return activeDialect().executeCustomUpdate(sql, params);
    }
}
