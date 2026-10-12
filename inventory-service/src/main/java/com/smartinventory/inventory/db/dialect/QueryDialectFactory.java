package com.smartinventory.inventory.db.dialect;

import com.smartinventory.inventory.db.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory class that polymorphically resolves and returns the appropriate {@link QueryDialect}
 * implementation based on the configured {@link DatabaseType}.
 */
@Service
public class QueryDialectFactory {

    private static final Logger log = LoggerFactory.getLogger(QueryDialectFactory.class);

    private final Map<DatabaseType, QueryDialect> dialectMap = new EnumMap<>(DatabaseType.class);

    @Autowired
    public QueryDialectFactory(List<AbstractQueryDialect> dialects) {
        if (dialects != null) {
            for (AbstractQueryDialect dialect : dialects) {
                if (dialect.getDatabaseType() != null) {
                    dialectMap.put(dialect.getDatabaseType(), dialect);
                }
            }
        }
        log.info("Initialized QueryDialectFactory with {} registered dialects: {}",
                dialectMap.size(), dialectMap.keySet());
    }

    /**
     * Fallback constructor creating default dialect instances when Spring Context is not active.
     */
    public QueryDialectFactory() {
        this(List.of(
                new PostgresQueryDialect(null),
                new MySqlQueryDialect(null),
                new OracleQueryDialect(null),
                new SqlServerQueryDialect(null),
                new Db2QueryDialect(null)
        ));
    }

    /**
     * Retrieves the database query dialect for a specified {@link DatabaseType}.
     */
    public QueryDialect getDialect(DatabaseType databaseType) {
        DatabaseType targetType = (databaseType != null) ? databaseType : DatabaseType.POSTGRESQL;
        QueryDialect dialect = dialectMap.get(targetType);
        if (dialect == null) {
            log.warn("No dialect found for DB type {}. Falling back to PostgreSQL dialect.", targetType);
            return dialectMap.get(DatabaseType.POSTGRESQL);
        }
        return dialect;
    }

    /**
     * Retrieves the database query dialect for a database type string.
     */
    public QueryDialect getDialect(String dbTypeStr) {
        return getDialect(DatabaseType.fromString(dbTypeStr));
    }
}
