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

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    id("java")
}

group = "com.smartinventory"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation(libs.querydsl.sql)
    implementation(libs.spring.boot.starter.security)
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.springdoc.openapi)
    implementation(libs.commons.lang3)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation(libs.postgresql)
    implementation(libs.mysql)
    implementation(libs.oracle)
    implementation(libs.mssql)
    implementation(libs.db2)
    runtimeOnly(libs.h2)
    implementation(libs.hikaricp)
    implementation(libs.spring.kafka)
    implementation(libs.micrometer.prometheus)
    implementation(libs.spring.boot.starter.actuator)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.spring.security.test)
}

tasks.register<JavaExec>("runMigrationSeeder") {
    group = "application"
    description = "Runs the standalone Database Migration & Data Seeding Tool independently."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.smartinventory.inventory.tool.MigrationSeederTool")
    args = listOf("--config=config/seeder-migration.properties", "--mode=all")
}

tasks.register<Jar>("buildSeederJar") {
    group = "build"
    description = "Builds a standalone executable fat JAR for the Migration & Seeding Tool."
    archiveFileName.set("migration-seeder-tool.jar")
    destinationDirectory.set(file("${rootDir}/bin"))

    manifest {
        attributes(
            "Main-Class" to "com.smartinventory.inventory.tool.MigrationSeederTool"
        )
    }

    val runtimeClasspath = configurations.runtimeClasspath.get()
    from(sourceSets.main.get().output)
    from({
        runtimeClasspath.filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Exec>("buildWebUi") {
    group = "build"
    description = "Builds the modern React 19 SPA frontend from web-ui/ into inventory-service static resources."
    workingDir = file("${rootDir}/web-ui")
    if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
        commandLine("cmd", "/c", "npm install && npm run build")
    } else {
        commandLine("sh", "-c", "npm install && npm run build")
    }
    isIgnoreExitValue = true
}

