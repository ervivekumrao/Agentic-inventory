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
