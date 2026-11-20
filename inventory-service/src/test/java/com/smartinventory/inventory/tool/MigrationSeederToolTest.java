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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MigrationSeederToolTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should run standalone MigrationSeederTool successfully against H2 in-memory database")
    void main_ShouldExecuteSuccessfullyWithH2() throws IOException {
        File configFile = tempDir.resolve("seeder-test.properties").toFile();
        Properties props = new Properties();
        props.setProperty("db.url", "jdbc:h2:mem:tooltestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        props.setProperty("db.username", "sa");
        props.setProperty("db.password", "");
        props.setProperty("db.driver-class-name", "org.h2.Driver");
        props.setProperty("migration.enabled", "true");
        props.setProperty("migration.baseline-on-migrate", "true");
        props.setProperty("migration.locations", "classpath:db/migration");
        props.setProperty("security.init-users", "true");
        props.setProperty("seeding.enabled", "true");
        props.setProperty("seeding.file-path", "samples/inventory_import_template.csv");

        try (FileWriter writer = new FileWriter(configFile)) {
            props.store(writer, "Test Config");
        }

        String[] args = new String[]{
                "--config=" + configFile.getAbsolutePath(),
                "--mode=all"
        };

        assertDoesNotThrow(() -> MigrationSeederTool.main(args));
    }
}
