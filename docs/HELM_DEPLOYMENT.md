# Helm Chart Deployment & Distributed Systems Configuration

This guide details how the Smart Inventory Management System supports distributed deployments, parameterizable endpoints, and Helm chart packaging, removing previous hardcoded `localhost` and port assumptions.

---

## 🛠️ 1. Elimination of Hardcoded `localhost` & Ports

Previously, microservices and configuration files hardcoded `localhost` and default ports (e.g., `localhost:5432`, `localhost:9092`, `localhost:8082`, `localhost:8083`). We have refactored all components to support environment variable overrides with sensible local defaults:

### A. Spring Boot Microservices (`inventory-service`, `catalog-service`)
- **Database & Kafka Connections**: Configured via environment variables with defaults:
  - `DB_HOST` / `DB_PORT` (default: `localhost:5432` for PostgreSQL)
  - `MONGO_HOST` / `MONGO_PORT` (default: `localhost:27017` for MongoDB)
  - `KAFKA_HOST` / `KAFKA_PORT` (default: `localhost:9092` for Apache Kafka)
- **Inter-Service Communication URLs**:
  - `FORECASTING_SERVICE_URL` (default: `http://localhost:8083`)
  - `CATALOG_SERVICE_URL` (default: `http://localhost:8082`)

### B. Python AI Forecasting Service (`forecasting-service`)
- Communicates with Inventory and Catalog services using environment variables:
  - `INVENTORY_SERVICE_URL` (default: `http://localhost:8081`)
  - `CATALOG_SERVICE_URL` (default: `http://localhost:8082`)

### C. Sample Data Seeder Tool (`scripts/seed_data.py`)
- Reads target endpoints from environment variables (`CATALOG_URL`, `INVENTORY_URL`, etc.) with fallback to `localhost`.

---

## ⛵ 2. Helm Chart Architecture (`infra/helm/smart-inventory`)

A production-ready Helm chart has been created at [`infra/helm/smart-inventory`](file:///home/user/work/smart-inventory-gradle-java/infra/helm/smart-inventory) to deploy the distributed system to any Kubernetes cluster.

### Chart Structure:
```text
infra/helm/smart-inventory/
├── Chart.yaml                  # Chart metadata
├── values.yaml                 # Configurable endpoints, replicas, image tags, resources
└── templates/
    ├── _helpers.tpl            # Template helper functions
    ├── inventory-service.yaml  # Inventory Service Deployment & ClusterIP Service
    ├── catalog-service.yaml    # Catalog Service Deployment & ClusterIP Service
    ├── forecasting-service.yaml# AI Forecasting Service Deployment & ClusterIP Service
    └── ingress.yaml            # Optional Ingress routing rules
```

---

## 🚀 3. Usage & Deployment Instructions

### Step 1: Build & Push Docker Images to your Cluster Registry
```bash
# Build Docker images locally
docker build -t smart-inventory-inventory-service:latest ./inventory-service
docker build -t smart-inventory-catalog-service:latest ./catalog-service
docker build -t smart-inventory-forecasting-service:latest ./forecasting-service
```

### Step 2: Customize `values.yaml` for your Distributed Environment
In [`infra/helm/smart-inventory/values.yaml`](file:///home/user/work/smart-inventory-gradle-java/infra/helm/smart-inventory/values.yaml), configure:
- External or cluster-internal database endpoints (`postgres-postgresql:5432`, `mongodb:27017`, `kafka:9092`)
- Replica counts and resource limits
- Ingress hostnames and paths

### Step 3: Deploy using Helm
```bash
# Install the Helm chart
helm install smart-inventory ./infra/helm/smart-inventory

# Upgrade existing deployment
helm upgrade smart-inventory ./infra/helm/smart-inventory
```

### Step 4: Verify Deployment & Endpoints
```bash
kubectl get pods
kubectl get services
kubectl get ingress
```
