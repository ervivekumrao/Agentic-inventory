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

# Smart Inventory System - Module Container Reload Utility
# Applies code changes to running Podman/Docker containers without bringing down databases or brokers.

set -e

SERVICE_NAME=$1

USAGE="Usage: ./scripts/reload_service.sh [inventory-service | catalog-service | forecasting-service | all]"

if [ -z "$SERVICE_NAME" ]; then
    echo "❌ Error: Service name required."
    echo "$USAGE"
    exit 1
fi

COMPOSE_FILE="infra/docker-compose.dev.yml"

# Detect container engine and compose CLI command (Podman is default)
detect_container_engine() {
    # Ensure Podman has unqualified-search-registries configured for docker.io
    if [ ! -f "$HOME/.config/containers/registries.conf" ]; then
        mkdir -p "$HOME/.config/containers"
        cat <<'REG_EOF' > "$HOME/.config/containers/registries.conf"
unqualified-search-registries = ["docker.io"]
REG_EOF
    fi

    if [ -n "$COMPOSE_CMD" ]; then
        DOCKER_COMPOSE="$COMPOSE_CMD"
        echo "📦 Using custom container compose command: $DOCKER_COMPOSE"
        return
    fi

    # Explicit engine override
    if [ "$CONTAINER_ENGINE" = "docker" ]; then
        if command -v docker-compose &> /dev/null; then
            DOCKER_COMPOSE="docker-compose"
        elif command -v docker &> /dev/null; then
            DOCKER_COMPOSE="docker compose"
        else
            echo "❌ Error: CONTAINER_ENGINE=docker was specified, but Docker was not found on this system."
            exit 1
        fi
        echo "📦 Using container engine: $DOCKER_COMPOSE (explicitly selected)"
        return
    fi

    if [ "$CONTAINER_ENGINE" = "podman" ]; then
        if command -v podman &> /dev/null && podman compose version &> /dev/null 2>&1; then
            DOCKER_COMPOSE="podman compose"
        elif command -v podman-compose &> /dev/null; then
            DOCKER_COMPOSE="podman-compose"
        else
            echo "❌ Error: CONTAINER_ENGINE=podman was specified, but Podman was not found."
            echo "   Please install Podman:"
            echo "     - Ubuntu/Debian: sudo apt update && sudo apt install -y podman podman-compose"
            echo "     - Fedora/RHEL:   sudo dnf install -y podman podman-compose"
            echo "     - macOS:         brew install podman podman-compose"
            exit 1
        fi
        echo "📦 Using container engine: $DOCKER_COMPOSE"
        if command -v systemctl &> /dev/null; then
            systemctl --user start podman.socket &> /dev/null || true
        fi
        return
    fi

    # Default is Podman
    if command -v podman &> /dev/null && podman compose version &> /dev/null 2>&1; then
        DOCKER_COMPOSE="podman compose"
        echo "📦 Using default container engine: $DOCKER_COMPOSE"
        if command -v systemctl &> /dev/null; then
            systemctl --user start podman.socket &> /dev/null || true
        fi
    elif command -v podman-compose &> /dev/null; then
        DOCKER_COMPOSE="podman-compose"
        echo "📦 Using default container engine: $DOCKER_COMPOSE"
        if command -v systemctl &> /dev/null; then
            systemctl --user start podman.socket &> /dev/null || true
        fi
    else
        # Podman is not detected. Check for Docker fallback.
        local has_docker=false
        local docker_cmd=""

        if command -v docker-compose &> /dev/null; then
            has_docker=true
            docker_cmd="docker-compose"
        elif command -v docker &> /dev/null; then
            has_docker=true
            docker_cmd="docker compose"
        fi

        if [ "$has_docker" = true ]; then
            echo "=========================================================================="
            echo "⚠️  Notice: Podman is the default container engine for this project, but"
            echo "   Podman was not detected on your system."
            echo ""
            echo "   👉 Recommended: Install Podman for daemonless, rootless containers:"
            echo "      - Ubuntu/Debian: sudo apt update && sudo apt install -y podman podman-compose"
            echo "      - Fedora/RHEL:   sudo dnf install -y podman podman-compose"
            echo "      - macOS:         brew install podman podman-compose && podman machine init && podman machine start"
            echo ""
            echo "   🐳 Docker detected ($docker_cmd). Continuing with Docker..."
            echo "=========================================================================="
            DOCKER_COMPOSE="$docker_cmd"
        else
            echo "=========================================================================="
            echo "❌ Error: No container engine found!"
            echo "   Podman is the default container engine for this project."
            echo ""
            echo "   Please install Podman:"
            echo "     - Ubuntu/Debian: sudo apt update && sudo apt install -y podman podman-compose"
            echo "     - Fedora/RHEL:   sudo dnf install -y podman podman-compose"
            echo "     - macOS:         brew install podman podman-compose && podman machine init && podman machine start"
            echo ""
            echo "   Alternatively, install Docker or set COMPOSE_CMD=\"...\""
            echo "=========================================================================="
            exit 1
        fi
    fi
}

detect_container_engine

if [ -f ".env" ]; then
    ENV_ARG="--env-file .env"
else
    ENV_ARG=""
fi

case "$SERVICE_NAME" in
    inventory-service)
        echo "🔄 Rebuilding & applying changes to [inventory-service] via $DOCKER_COMPOSE..."
        ./gradlew :inventory-service:build -x test
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build inventory-service
        echo "✅ [inventory-service] updated and running!"
        ;;
    catalog-service)
        echo "🔄 Rebuilding & applying changes to [catalog-service] via $DOCKER_COMPOSE..."
        ./gradlew :catalog-service:build -x test
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build catalog-service
        echo "✅ [catalog-service] updated and running!"
        ;;
    forecasting-service)
        echo "🔄 Rebuilding & applying changes to [forecasting-service] via $DOCKER_COMPOSE..."
        $DOCKER_COMPOSE $ENV_ARG -f $COMPOSE_FILE up -d --build forecasting-service
        echo "✅ [forecasting-service] updated and running!"
        ;;
    all)
        echo "🔄 Rebuilding & applying changes to ALL microservices via $DOCKER_COMPOSE..."
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
