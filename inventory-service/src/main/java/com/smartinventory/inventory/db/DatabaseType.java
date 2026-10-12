package com.smartinventory.inventory.db;

public enum DatabaseType {
    POSTGRESQL("org.hibernate.dialect.PostgreSQLDialect", "jdbc:postgresql://localhost:5432/inventory"),
    MYSQL("org.hibernate.dialect.MySQLDialect", "jdbc:mysql://localhost:3306/inventory"),
    ORACLE("org.hibernate.dialect.OracleDialect", "jdbc:oracle:thin:@localhost:1521:ORCL"),
    MSSQL("org.hibernate.dialect.SQLServerDialect", "jdbc:sqlserver://localhost:1433;databaseName=inventory"),
    DB2("org.hibernate.dialect.DB2Dialect", "jdbc:db2://localhost:50000/inventory");

    private final String hibernateDialect;
    private final String defaultUrl;

    DatabaseType(String hibernateDialect, String defaultUrl) {
        this.hibernateDialect = hibernateDialect;
        this.defaultUrl = defaultUrl;
    }

    public String getHibernateDialect() {
        return hibernateDialect;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public static DatabaseType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return POSTGRESQL;
        }
        try {
            return DatabaseType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return POSTGRESQL;
        }
    }
}
