<!--
  Copyright (c) 2026 Vivek Umrao.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# Helm Chart Deployment & Distributed Systems Configuration

This guide details how the Smart Inventory Management System deploys onto Kubernetes clusters using parameterizable endpoints, containerized microservices, and modern Helm packaging.

---

## 🛠️ 1. Dynamic Endpoint Parameterization

All microservices and tools support environment variable overrides with sensible local defaults:

### A. Spring Boot Microservices (`inventory-service`, `catalog-service`)
- **Database & Kafka Connections**: Configured via environment variables:
  - `DB_HOST` / `DB_PORT` (default: `localhost:5432` for PostgreSQL)
  - `MONGO_HOST` / `MONGO_PORT` (default: `localhost:27017` for MongoDB)
  - `KAFKA_HOST` / `KAFKA_PORT` (default: `localhost:9092` for Apache Kafka)
- **Inter-Service Communication**:
  - `FORECASTING_SERVICE_URL` (default: `http://localhost:8083`)
  - `CATALOG_SERVICE_URL` (default: `http://localhost:8082`)

### B. Python AI Forecasting Service (`forecasting-service`)
- Communicates with Inventory and Catalog services using environment variables:
  - `INVENTORY_SERVICE_URL` (default: `http://localhost:8081`)
  - `CATALOG_SERVICE_URL` (default: `http://localhost:8082`)

### C. Sample Data Seeder Tool (`scripts/seed_data.py`)
- Reads target endpoints from `CATALOG_URL` and `INVENTORY_URL` with automatic local fallback.

---

## ⛵ 2. Helm Chart Architecture (`infra/helm/smart-inventory`)

A production-ready Helm chart is provided at [`infra/helm/smart-inventory`](file:///home/user/work/Agentic-inventory/infra/helm/smart-inventory):

### Chart Structure:
```text
infra/helm/smart-inventory/
├── Chart.yaml                  # Chart metadata (version 1.0.0)
├── values.yaml                 # Configurable endpoints, replicas, image tags, resources
└── templates/
    ├── _helpers.tpl            # Template helper functions & labels
    ├── catalog-service.yaml    # Deployment & Service for catalog-service
    ├── forecasting-service.yaml# Deployment & Service for forecasting-service
    ├── inventory-service.yaml  # Deployment & Service for inventory-service
    └── ingress.yaml            # Ingress rules routing /api/inventory, /api/catalog, /api/ai
```

---

## ⚙️ 3. Key Configuration Parameters (`values.yaml`)

| Parameter | Default | Description |
| :--- | :--- | :--- |
| `inventoryService.replicaCount` | `1` | Number of inventory-service pods |
| `inventoryService.image.repository` | `smart-inventory/inventory-service` | Container image repository |
| `inventoryService.image.tag` | `latest` | Container image tag |
| `inventoryService.database.type` | `POSTGRESQL` | Active dialect: `POSTGRESQL`, `MYSQL`, `ORACLE`, `MSSQL`, `DB2` |
| `inventoryService.database.host` | `postgres-service` | Database hostname |
| `inventoryService.database.port` | `5432` | Database port |
| `catalogService.replicaCount` | `1` | Number of catalog-service pods |
| `catalogService.mongodb.host` | `mongodb-service` | MongoDB hostname |
| `catalogService.mongodb.port` | `27017` | MongoDB port |
| `forecastingService.replicaCount` | `1` | Number of forecasting-service pods |
| `ingress.enabled` | `false` | Enable Kubernetes Ingress controller |
| `ingress.host` | `inventory.local` | Ingress hostname |

---

## 🚀 4. Deployment Instructions

### 1. Validate & Lint Helm Chart
```bash
helm lint infra/helm/smart-inventory
```

### 2. Dry-Run Installation (Template Rendering Test)
```bash
helm install smart-inventory infra/helm/smart-inventory --dry-run --debug
```

### 3. Deploy to Kubernetes Cluster
```bash
helm install smart-inventory infra/helm/smart-inventory \
  --namespace inventory \
  --create-namespace
```

### 4. Deploy with Custom Database Host & Port (e.g., MySQL)
```bash
helm install smart-inventory infra/helm/smart-inventory \
  --namespace inventory \
  --set inventoryService.database.type=MYSQL \
  --set inventoryService.database.host=mysql-cluster.db.svc.cluster.local \
  --set inventoryService.database.port=3306
```

### 5. Verify Pod Rollout & Health
```bash
kubectl get pods -n inventory
kubectl get services -n inventory
```

### 6. Upgrade Release
```bash
helm upgrade smart-inventory infra/helm/smart-inventory --namespace inventory
```

### 7. Uninstall Release
```bash
helm uninstall smart-inventory --namespace inventory
```

---

## 📜 5. License & Copyright

```text
Copyright (c) 2026 Vivek Umrao.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
