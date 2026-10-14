#!/usr/bin/env bash
# Copyright (c) 2026 Vivek Umrao.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Smart Inventory System - Module Docker Reload Utility
# Applies code changes to running Docker containers without bringing down databases or brokers.

set -e

SERVICE_NAME=$1

USAGE="Usage: ./scripts/reload_service.sh [inventory-service | catalog-service | forecasting-service | all]"

if [ -z "$SERVICE_NAME" ]; then
    echo "❌ Error: Service name required."
    echo "$USAGE"
    exit 1
fi

COMPOSE_FILE="infra/docker-compose.dev.yml"

# Detect docker-compose CLI command
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    DOCKER_COMPOSE="docker compose"
fi

if [ -f ".env" ]; then
    ENV_ARG="--env-file .env"
else
    ENV_ARG=""
fi

case "$SERVICE_NAME" in
    inventory-service)
        echo "🔄 Rebuilding & applying changes to [inventory-service]..."
        ./gradlew :inventory-service:build -x test
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build inventory-service
        echo "✅ [inventory-service] updated and running!"
        ;;
    catalog-service)
        echo "🔄 Rebuilding & applying changes to [catalog-service]..."
        ./gradlew :catalog-service:build -x test
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build catalog-service
        echo "✅ [catalog-service] updated and running!"
        ;;
    forecasting-service)
        echo "🔄 Rebuilding & applying changes to [forecasting-service]..."
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build forecasting-service
        echo "✅ [forecasting-service] updated and running!"
        ;;
    all)
        echo "🔄 Rebuilding & applying changes to ALL microservices..."
        ./gradlew :inventory-service:build :catalog-service:build -x test
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build
        echo "✅ All microservices updated and running!"
        ;;
    *)
        echo "❌ Unknown service: $SERVICE_NAME"
        echo "$USAGE"
        exit 1
        ;;
esac
