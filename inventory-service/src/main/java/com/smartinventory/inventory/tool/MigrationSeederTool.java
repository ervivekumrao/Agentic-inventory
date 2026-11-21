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

package com.smartinventory.inventory.tool;

import com.smartinventory.inventory.dto.BulkImportResult;
import com.smartinventory.inventory.importer.BulkDataImporter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Standalone Database Migration and Data Seeding Utility Tool.
 * Executable independently from external configuration files (e.g. config/seeder-migration.properties)
 * or command-line parameters without starting application web servers.
 */
public class MigrationSeederTool {

    private static final Logger log = LoggerFactory.getLogger(MigrationSeederTool.class);

    public static void main(String[] args) {
        String configFile = parseArg(args, "--config=", "config/seeder-migration.properties");
        String mode = parseArg(args, "--mode=", "all").toLowerCase();

        Properties props = loadProperties(configFile);

        String logbackConfig = getProperty(props, "logging.config", "LOGBACK_CONFIG", "config/logback-seeder.xml");
        File lFile = new File(logbackConfig);
        if (lFile.exists()) {
            System.setProperty("logback.configurationFile", lFile.getAbsolutePath());
        }
        File logsDir = new File("logs");
        if (!logsDir.exists()) {
            boolean created = logsDir.mkdirs();
            if (!created) {
                log.debug("Log directory already exists or creation deferred.");
            }
        }

        log.info("🛠️ Starting Standalone Migration & Data Seeding Tool...");
        log.info("📄 Mode: {}, Config File: {}", mode, configFile);

        String dbUrl = getProperty(props, "db.url", "SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/inventory");
        String dbUser = getProperty(props, "db.username", "SPRING_DATASOURCE_USERNAME", "inventory");
        String dbPass = getProperty(props, "db.password", "SPRING_DATASOURCE_PASSWORD", "inventory");
        String dbDriver = getProperty(props, "db.driver-class-name", "SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPass);
        hikariConfig.setDriverClassName(dbDriver);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setAutoCommit(false);

        try (HikariDataSource dataSource = new HikariDataSource(hikariConfig)) {
            log.info("✅ Database Connection Pool Established: {}", dbUrl);

            // 1. Run Flyway Migrations
            if ("migrate".equals(mode) || "all".equals(mode)) {
                boolean migrationEnabled = Boolean.parseBoolean(props.getProperty("migration.enabled", "true"));
                if (migrationEnabled) {
                    runFlywayMigrations(dataSource, props);
                } else {
                    log.info("ℹ️ Flyway migrations disabled in configuration.");
                }
            }

            // 2. Run Security Roles/Users & Data Seeding
            if ("seed".equals(mode) || "all".equals(mode)) {
                boolean securityEnabled = Boolean.parseBoolean(props.getProperty("security.init-users", "true"));
                if (securityEnabled) {
                    seedSecurityRolesAndUsers(dataSource, props);
                }

                boolean seedingEnabled = Boolean.parseBoolean(props.getProperty("seeding.enabled", "true"));
                if (seedingEnabled) {
                    runBulkDataSeeding(dataSource, props);
                } else {
                    log.info("ℹ️ Bulk inventory data seeding disabled in configuration.");
                }
            }

            log.info("🎉 Standalone Migration & Seeding Tool finished successfully!");
        } catch (Exception e) {
            log.error("❌ Migration & Seeding Tool execution failed: {}", e.getMessage(), e);
            throw new RuntimeException("Migration & Seeding Tool execution failed: " + e.getMessage(), e);
        }
    }

    private static void runFlywayMigrations(HikariDataSource dataSource, Properties props) {
        log.info("🔄 Running Flyway Database Migrations...");
        boolean baselineOnMigrate = Boolean.parseBoolean(props.getProperty("migration.baseline-on-migrate", "true"));
        String locationsProp = props.getProperty("migration.locations", "classpath:db/migration,filesystem:inventory-service/src/main/resources/db/migration,filesystem:src/main/resources/db/migration");
        String[] locs = locationsProp.split(",");

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(baselineOnMigrate)
                    .locations(locs)
                    .validateMigrationNaming(false)
                    .load();

            int migrationsApplied = flyway.migrate().migrationsExecuted;
            log.info("✅ Flyway Migrations completed successfully. Applied {} migration scripts.", migrationsApplied);
        } catch (Exception e) {
            log.warn("Flyway migration runner encountered warning: {}", e.getMessage());
        }

        try (Connection conn = dataSource.getConnection()) {
            if (!recordExists(conn, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", "roles")) {
                log.info("📄 Executing initial database schema migrations directly via JDBC fallback...");
                executeDirectMigrations(dataSource);
            }
        } catch (Exception e) {
            log.error("Failed schema check or direct DDL fallback: {}", e.getMessage(), e);
        }
    }

    private static void executeDirectMigrations(HikariDataSource dataSource) {
        String[] scripts = {
            "db/migration/V1__init_schema.sql",
            "db/migration/V2__multi_location_stock_positions.sql",
            "db/migration/V3__add_domain_expansion_tables.sql",
            "db/migration/V4__reservation_ownership.sql",
            "db/migration/V5__pgvector_feedback.sql"
        };
        for (String script : scripts) {
            try {
                String sql = null;
                InputStream is = MigrationSeederTool.class.getClassLoader().getResourceAsStream(script);
                if (is != null) {
                    sql = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    File file = new File("inventory-service/src/main/resources/" + script);
                    if (!file.exists()) {
                        file = new File("src/main/resources/" + script);
                    }
                    if (file.exists()) {
                        sql = java.nio.file.Files.readString(file.toPath());
                    }
                }
                if (sql != null && !sql.isBlank()) {
                    try (Connection conn = dataSource.getConnection();
                         java.sql.Statement stmt = conn.createStatement()) {
                        conn.setAutoCommit(true);
                        for (String part : sql.split(";")) {
                            String trimmed = part.trim();
                            if (!trimmed.isBlank()) {
                                try {
                                    stmt.execute(trimmed);
                                } catch (Exception ex) {
                                    log.debug("Statement execution note: {}", ex.getMessage());
                                }
                            }
                        }
                        log.info("✅ Executed migration script: {}", script);
                    }
                }
            } catch (Exception e) {
                log.warn("Migration script {} execution note: {}", script, e.getMessage());
            }
        }
    }

    private static void seedSecurityRolesAndUsers(HikariDataSource dataSource, Properties props) throws SQLException {
        log.info("🔐 Initializing Security Roles & Default Administrative Users...");
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String adminPass = props.getProperty("security.admin-password", "admin123");
        String managerPass = props.getProperty("security.manager-password", "manager123");
        String operatorPass = props.getProperty("security.operator-password", "operator123");
        String viewerPass = props.getProperty("security.viewer-password", "viewer123");

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // Seed Roles
            String[] roles = {"ROLE_ADMIN", "ROLE_MANAGER", "ROLE_ENTRY_OPERATOR", "ROLE_USER"};
            for (String roleName : roles) {
                if (!recordExists(conn, "SELECT COUNT(*) FROM roles WHERE name = ?", roleName)) {
                    try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO roles (name, description) VALUES (?, ?)")) {
                        pstmt.setString(1, roleName);
                        pstmt.setString(2, "Default " + roleName);
                        pstmt.executeUpdate();
                    }
                }
            }

            // Seed Users
            seedUserIfMissing(conn, passwordEncoder, "admin", adminPass, "ROLE_ADMIN");
            seedUserIfMissing(conn, passwordEncoder, "manager", managerPass, "ROLE_MANAGER");
            seedUserIfMissing(conn, passwordEncoder, "operator", operatorPass, "ROLE_ENTRY_OPERATOR");
            seedUserIfMissing(conn, passwordEncoder, "viewer", viewerPass, "ROLE_USER");

            conn.commit();
            log.info("✅ Security Roles and Users initialized successfully.");
        }
    }

    private static void seedUserIfMissing(Connection conn, PasswordEncoder encoder, String username, String rawPassword, String roleName) throws SQLException {
        if (!recordExists(conn, "SELECT COUNT(*) FROM users WHERE username = ?", username)) {
            long userId;
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (username, password, enabled) VALUES (?, ?, true)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, username);
                pstmt.setString(2, encoder.encode(rawPassword));
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        userId = rs.getLong(1);
                    } else {
                        return;
                    }
                }
            }

            long roleId = getRoleId(conn, roleName);
            if (roleId != -1) {
                try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                    pstmt.setLong(1, userId);
                    pstmt.setLong(2, roleId);
                    pstmt.executeUpdate();
                }
            }
        }
    }

    private static boolean recordExists(Connection conn, String sql, String param) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, param);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static long getRoleId(Connection conn, String roleName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM roles WHERE name = ?")) {
            pstmt.setString(1, roleName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return -1;
    }

    private static void runBulkDataSeeding(HikariDataSource dataSource, Properties props) {
        String filePath = props.getProperty("seeding.file-path", "samples/inventory_import_template.csv");
        log.info("🌱 Executing Bulk Reference Data Import from file: {}", filePath);

        File seedFile = new File(filePath);
        if (!seedFile.exists()) {
            log.warn("⚠️ Seed CSV file not found at: {}. Skipping inventory data import.", seedFile.getAbsolutePath());
            return;
        }

        try (InputStream inputStream = new FileInputStream(seedFile)) {
            BulkDataImporter importer = new BulkDataImporter(dataSource);
            BulkImportResult result = importer.importFromCsvStream(inputStream);
            log.info("✅ Bulk Reference Data Import finished: {} records imported in {} ms (Status: {})",
                    result.getImportedRecords(), result.getDurationMs(), result.isSuccess());
        } catch (Exception e) {
            log.error("❌ Error reading seed file: {}", e.getMessage(), e);
        }
    }

    private static Properties loadProperties(String configFile) {
        Properties props = new Properties();
        File file = new File(configFile);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
                log.info("📄 Loaded configuration file from: {}", file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Failed to load config file: {}. Falling back to default properties/env variables.", configFile);
            }
        } else {
            log.info("ℹ️ Config file {} not found on filesystem. Using environment variable defaults.", configFile);
        }
        return props;
    }

    private static String getProperty(Properties props, String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return props.getProperty(propKey, defaultValue);
    }

    private static String parseArg(String[] args, String prefix, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return defaultValue;
    }
}
