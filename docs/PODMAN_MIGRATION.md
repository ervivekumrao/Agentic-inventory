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

# Podman Migration & Execution Guide

This guide details how to build, run, and test the **Smart Inventory Management System** using **Podman** as a rootless, daemonless alternative to Docker.

---

## 1. Overview & Architecture

Both Docker and Podman implement the **Open Container Initiative (OCI)** image and runtime specifications. Consequently:
* **Zero Application Code Changes**: Controllers ([`UserManagementController.java`](../inventory-service/src/main/java/com/smartinventory/inventory/controller/UserManagementController.java)), models, repositories, and services run identically inside JVM/Python runtimes.
* **Reusable Dockerfiles**: Existing Dockerfiles ([`inventory-service/Dockerfile`](../inventory-service/Dockerfile), [`catalog-service/Dockerfile`](../catalog-service/Dockerfile), [`forecasting-service/Dockerfile`](../forecasting-service/Dockerfile)) build natively with `podman build`.
* **Rootless Security**: Podman operates without a background root daemon. Containers run in user namespaces where the in-container `appuser` (UID 10001) maps securely to unprivileged user permissions on the host system.

---

## 2. Installation & Prerequisites

### Linux (Ubuntu / Debian)
```bash
sudo apt update
sudo apt install -y podman podman-compose
```

### Linux (Fedora / RHEL / CentOS Stream)
```bash
sudo dnf install -y podman podman-compose
```

### macOS / Windows
Install [Podman Desktop](https://podman-desktop.io/) or use the CLI:
```bash
# macOS (Homebrew)
brew install podman podman-compose
podman machine init
podman machine start
```

---

## 3. Running with Podman

### Default Container Engine & Automatic Fallback
**Podman is the default container engine** for this project.

The automation scripts ([`scripts/setup.sh`](../scripts/setup.sh) and [`scripts/reload_service.sh`](../scripts/reload_service.sh)) handle detection automatically:
1. **Podman Present (Default)**: Automatically runs with `podman compose`. Also ensures `~/.config/containers/registries.conf` and the user socket (`podman.socket`) are active.
2. **Podman Missing, Docker Present**: Notifies the developer to install Podman with recommended commands, but gracefully continues execution using Docker so developer workflows are not interrupted.
3. **Neither Present**: Prompts the developer with installation commands for Podman and halts.

You can explicitly override the engine if desired:
```bash
# Explicitly enforce Podman or Docker:
export CONTAINER_ENGINE=podman   # or docker
# Or provide a custom compose command:
export COMPOSE_CMD="podman compose"
```

### Option A: Using the Automated Setup Script (Recommended)
```bash
# Fresh build & start with PostgreSQL and sample data seeding
./scripts/setup.sh fresh postgres

# Start existing environment
./scripts/setup.sh up postgres

# Hot-reload code changes into running container
./scripts/reload_service.sh inventory-service

# Stop containers (preserves database volumes)
./scripts/setup.sh destroy

# Clean all containers, networks, and images
./scripts/setup.sh clean
```

### Option B: Using `podman compose` Directly
Run the stack using the compose file:
```bash
# Start PostgreSQL stack
podman compose --profile postgres --env-file .env -f infra/docker-compose.dev.yml up --build -d

# Check running containers
podman ps

# View service logs
podman compose -f infra/docker-compose.dev.yml logs -f inventory-service

# Teardown
podman compose --profile "*" -f infra/docker-compose.dev.yml down
```

### Option C: Using Native Kubernetes Pods (`podman play kube`)
Podman can run containers together inside a Kubernetes Pod manifest:
```bash
# 1. Build the microservice images locally
podman build -t localhost/inventory-service:latest -f inventory-service/Dockerfile inventory-service
podman build -t localhost/catalog-service:latest -f catalog-service/Dockerfile catalog-service
podman build -t localhost/forecasting-service:latest -f forecasting-service/Dockerfile forecasting-service

# 2. Launch the pod
podman play kube infra/podman/smart-inventory-pod.yaml

# 3. Check pod status
podman pod ps

# 4. Tear down the pod
podman play kube --down infra/podman/smart-inventory-pod.yaml
```

---

## 4. Testcontainers Integration with Podman

Testcontainers (used in integration tests) can interact with Podman via Podman's rootless system socket.

### Step 1: Enable the Podman Socket
```bash
systemctl --user enable --now podman.socket
```
Verify the socket is active:
```bash
systemctl --user status podman.socket
```

### Step 2: Configure Environment Variables
Add these variables to your shell profile (`~/.bashrc` or `~/.zshrc`) or CI configuration:
```bash
export DOCKER_HOST="unix:///run/user/$UID/podman/podman.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/run/user/$UID/podman/podman.sock"
```

### Step 3: Run the Integration Tests
```bash
./gradlew test
```

---

## 5. Podman vs. Docker Reference Cheat Sheet

| Action | Docker Command | Podman Command |
| :--- | :--- | :--- |
| Build Image | `docker build -t app .` | `podman build -t app .` |
| List Containers | `docker ps -a` | `podman ps -a` |
| View Logs | `docker logs -f <name>` | `podman logs -f <name>` |
| Execute Shell | `docker exec -it <name> sh` | `podman exec -it <name> sh` |
| Docker Compose Up | `docker compose up -d` | `podman compose up -d` |
| Pod Management | *Not natively supported* | `podman pod create`, `podman pod ps` |
| Run K8s Manifest | *Requires Minikube/Kind* | `podman play kube <manifest.yaml>` |

---

## 6. Known Differences & Troubleshooting

### Unqualified Image Name Resolution (`registries.conf`)
By default on fresh Linux installations, Podman requires unqualified short image names (e.g., `postgres:15`, `python:3.11-slim`) to have search registries defined. 

[`scripts/setup.sh`](../scripts/setup.sh) automatically ensures `~/.config/containers/registries.conf` is configured:
```toml
unqualified-search-registries = ["docker.io"]
```
If setting up manually, create this file to allow pulling and building images without prompting.

### Container Health Checks & Whitespace Tokenization
In Docker Compose, inline commands passed as `["CMD", "python", "-c", "import ..."]` are executed as a single shell string. In Podman's internal health check runner, argument strings can undergo whitespace tokenization, causing `python -c import` to be invoked and resulting in syntax errors.

To guarantee 100% portability across both Podman and Docker:
- Microservices use dedicated health check scripts (e.g., [`forecasting-service/healthcheck.py`](../forecasting-service/healthcheck.py)) or native HTTP checks (`curl -f http://localhost:.../actuator/health`).
- Compose healthchecks specify `test: ["CMD", "python", "/app/healthcheck.py"]` or `["CMD-SHELL", "..."]`.

### Multi-Profile Conditional Dependencies (`podman-compose` vs `podman compose`)
Standalone legacy python `podman-compose` does not support Docker Compose v2 syntax `depends_on: { required: false }` across inactive compose profiles (e.g., `mysql`, `mssql`, `oracle`, `db2`). 

Our automation scripts prioritize modern **`podman compose`** (which leverages the official compose plugin), fully resolving profiles and optional service dependencies.

### Idempotent Data Seeding (`seed_data.py`)
`inventory-service` and `catalog-service` include automated bootstrap initializers (`FastDataInitializer` and `CatalogDataInitializer`) that seed sample records on startup if the database is empty. 

[`scripts/seed_data.py`](../scripts/seed_data.py) is engineered to be fully idempotent: it checks for existing records before posting and verifies user accounts safely without failing on duplicate constraints.

### IBM DB2 Container Privileges
In [`infra/docker-compose.dev.yml`](../infra/docker-compose.dev.yml#L110), the `db2` profile specifies `privileged: true`. 
In rootless Podman, granting true kernel privileges requires rootful privileges. If you are developing against IBM DB2, use rootful Podman:
```bash
sudo podman compose --profile db2 -f infra/docker-compose.dev.yml up -d
```
All other database profiles (`postgres`, `mysql`, `mssql`, `oracle`) run smoothly in standard rootless Podman.

### SELinux Volume Relabeling
On distributions with SELinux enabled (RHEL, Fedora, CentOS), host bind mounts require the `:Z` suffix to allow container access:
```yaml
volumes:
  - ./my-data:/data:Z
```
Internal named volumes (e.g. `postgres_data`, `mongo_data`) are managed by Podman directly and do not require relabeling.
