#!/usr/bin/env bash
# Smart Inventory System - Unified Setup & Management Utility

set -e

COMPOSE_FILE="infra/docker-compose.dev.yml"
ENV_FILE=".env"

# Detect docker-compose CLI command
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
else
    DOCKER_COMPOSE="docker compose"
fi

usage() {
    echo "=================================================="
    echo " Smart Inventory Management System - Setup Utility"
    echo "=================================================="
    echo "Usage: ./scripts/setup.sh [command] [db_type] [options]"
    echo ""
    echo "Commands:"
    echo "  fresh [db_type]        Destroy existing env (including volumes) and create fresh (seeds data)"
    echo "  up [db_type]           Start environment (does not re-seed by default)"
    echo "  destroy                Stop and remove containers (preserves volumes)"
    echo "  clean                  Clean all containers, volumes, networks, and images related to project"
    echo "  reload [service]       Rebuild and reload service(s) (inventory-service, catalog-service, forecasting-service, all)"
    echo "  seed                   Run sample data seeding manually"
    echo ""
    echo "Supported DB Types:"
    echo "  postgres (default), mssql, mysql, oracle, db2"
    echo ""
    echo "Custom Environment Variables (optional):"
    echo "  DB_PORT, DB_USER, DB_PASSWORD"
    echo ""
    echo "Examples:"
    echo "  ./scripts/setup.sh fresh postgres"
    echo "  ./scripts/setup.sh fresh mssql"
    echo "  ./scripts/setup.sh reload inventory-service"
    echo "  ./scripts/setup.sh clean"
    exit 1
}

configure_env() {
    local db_type=$1
    if [ -z "$db_type" ]; then
        db_type="postgres"
    fi

    echo "⚙️ Configuring environment for DB: $db_type..."

    case "$db_type" in
        postgres)
            cat <<EOF > $ENV_FILE
COMPOSE_PROFILES=postgres
INVENTORY_DB_TYPE=POSTGRESQL
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:${DB_PORT:-5432}/inventory
SPRING_DATASOURCE_USERNAME=${DB_USER:-inventory}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-inventory}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-inventory}
DB_PASSWORD=${DB_PASSWORD:-inventory}
EOF
            ;;
        mssql)
            cat <<EOF > $ENV_FILE
COMPOSE_PROFILES=mssql
INVENTORY_DB_TYPE=MSSQL
SPRING_DATASOURCE_URL=jdbc:sqlserver://mssql:${DB_PORT:-1433};databaseName=master;trustServerCertificate=true;encrypt=true
SPRING_DATASOURCE_USERNAME=${DB_USER:-sa}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-Password123!}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.microsoft.sqlserver.jdbc.SQLServerDriver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.SQLServerDialect
DB_PORT=${DB_PORT:-1433}
DB_USER=${DB_USER:-sa}
DB_PASSWORD=${DB_PASSWORD:-Password123!}
EOF
            ;;
        mysql)
            cat <<EOF > $ENV_FILE
COMPOSE_PROFILES=mysql
INVENTORY_DB_TYPE=MYSQL
SPRING_DATASOURCE_URL=jdbc:mysql://mysql:${DB_PORT:-3306}/inventory?useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=${DB_USER:-inventory}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-inventory}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.MySQLDialect
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-inventory}
DB_PASSWORD=${DB_PASSWORD:-inventory}
EOF
            ;;
        oracle)
            cat <<EOF > $ENV_FILE
COMPOSE_PROFILES=oracle
INVENTORY_DB_TYPE=ORACLE
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@oracle:${DB_PORT:-1521}/FREEPDB1
SPRING_DATASOURCE_USERNAME=${DB_USER:-inventory}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-inventory}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=oracle.jdbc.OracleDriver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.OracleDialect
DB_PORT=${DB_PORT:-1521}
DB_USER=${DB_USER:-inventory}
DB_PASSWORD=${DB_PASSWORD:-inventory}
EOF
            ;;
        db2)
            cat <<EOF > $ENV_FILE
COMPOSE_PROFILES=db2
INVENTORY_DB_TYPE=DB2
SPRING_DATASOURCE_URL=jdbc:db2://db2:${DB_PORT:-50000}/invdb
SPRING_DATASOURCE_USERNAME=${DB_USER:-db2inst1}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD:-Password123!}
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.ibm.db2.jcc.DB2Driver
SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT=org.hibernate.dialect.DB2Dialect
DB_PORT=${DB_PORT:-50000}
DB_USER=${DB_USER:-db2inst1}
DB_PASSWORD=${DB_PASSWORD:-Password123!}
EOF
            ;;
        *)
            echo "❌ Unknown DB type: $db_type"
            exit 1
            ;;
    esac
    echo "✅ Environment configured for $db_type in $ENV_FILE"
}

# Parse command
COMMAND=$1
SHIFT_ARG=$2

# Handle legacy/direct DB type argument (e.g. ./scripts/setup.sh postgres)
if [ "$COMMAND" = "postgres" ] || [ "$COMMAND" = "mssql" ] || [ "$COMMAND" = "mysql" ] || [ "$COMMAND" = "oracle" ] || [ "$COMMAND" = "db2" ]; then
    DB_TYPE=$COMMAND
    COMMAND="fresh" # Default direct call to fresh setup & seed for convenience
elif [ -z "$COMMAND" ]; then
    usage
else
    DB_TYPE=$SHIFT_ARG
fi

case "$COMMAND" in
    fresh|recreate)
        if [ -z "$DB_TYPE" ]; then
            DB_TYPE="postgres"
        fi
        echo "🔄 Destroying existing environment (including volumes) and creating fresh [DB: $DB_TYPE]..."
        configure_env "$DB_TYPE"
        $DOCKER_COMPOSE --profile "*" --env-file $ENV_FILE -f $COMPOSE_FILE down -v --remove-orphans
        echo "🔨 Building Java microservices..."
        ./gradlew :inventory-service:build :catalog-service:build -x test
        echo "🚀 Starting containers..."
        $DOCKER_COMPOSE --env-file $ENV_FILE -f $COMPOSE_FILE up --build -d
        echo "🌱 Seeding initial sample data for new environment..."
        python3 scripts/seed_data.py
        echo "🎉 Fresh environment setup completed successfully!"
        ;;
    up)
        if [ -z "$DB_TYPE" ] && [ ! -f "$ENV_FILE" ]; then
            DB_TYPE="postgres"
            configure_env "$DB_TYPE"
        elif [ -n "$DB_TYPE" ]; then
            configure_env "$DB_TYPE"
        fi

        if [ -f "$ENV_FILE" ]; then
            ENV_ARG="--env-file $ENV_FILE"
        else
            ENV_ARG=""
        fi

        echo "🚀 Starting environment..."
        $DOCKER_COMPOSE "$ENV_ARG" -f $COMPOSE_FILE up --build -d

        # Check if user passed --seed
        if [[ "$*" == *"--seed"* ]]; then
            echo "🌱 Seeding data as requested..."
            python3 scripts/seed_data.py
        else
            echo "ℹ️ Started environment (data seeding skipped by default)."
            echo "   To seed data manually, run: ./scripts/setup.sh seed"
        fi
        echo "✅ Environment running!"
        ;;
    destroy)
        echo "🛑 Stopping and removing containers (preserving volumes)..."
        $DOCKER_COMPOSE --profile "*" -f $COMPOSE_FILE down
        echo "✅ Environment destroyed (volumes preserved)."
        ;;
    clean)
        echo "🧹 Cleaning all containers, volumes, networks, and images related to project..."
        $DOCKER_COMPOSE --profile "*" -f $COMPOSE_FILE down -v --rmi local --remove-orphans
        if [ -f "$ENV_FILE" ]; then
            rm -f "$ENV_FILE"
        fi
        echo "✅ Project completely cleaned!"
        ;;
    reload)
        SERVICE_NAME=$2
        if [ -z "$SERVICE_NAME" ]; then
            SERVICE_NAME="all"
        fi
        ./scripts/reload_service.sh "$SERVICE_NAME"
        ;;
    seed)
        python3 scripts/seed_data.py
        ;;
    *)
        echo "❌ Unknown command: $COMMAND"
        usage
        ;;
esac
