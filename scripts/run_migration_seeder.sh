#!/usr/bin/env bash
# Copyright (c) 2026 Vivek Umrao.
# Standalone Database Migration & Data Seeder Script (No Gradle at Runtime)

set -e

CONFIG_FILE="${1:-config/seeder-migration.properties}"
MODE="${2:-all}"

exec bin/run-migration-seeder.sh "$CONFIG_FILE" "$MODE"
