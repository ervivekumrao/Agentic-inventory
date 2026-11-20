@echo off
rem Copyright (c) 2026 Vivek Umrao.
rem Standalone Database Migration & Data Seeder Script (Windows - No Gradle at Runtime)

set CONFIG_FILE=%1
if "%CONFIG_FILE%"=="" set CONFIG_FILE=config/seeder-migration.properties

set MODE=%2
if "%MODE%"=="" set MODE=all

call bin\run-migration-seeder.bat "%CONFIG_FILE%" "%MODE%"
