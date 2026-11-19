# Project Guidelines & Rules for Agentic AI

## 1. Git Commit Message Conventions
- **NO Prefixes**: NEVER use Conventional Commit type prefixes (such as `feat:`, `fix:`, `chore:`, `feat(ai):`, `refactor:`, `style:`, etc.).
- **Plain Text Titles**: Use clean, direct, descriptive commit titles starting with a capital letter or imperative form (e.g., `Integrate FastMCP Server adapter into forecasting service`).

## 2. Container & Server Execution Environment
- **WSL Environment Only**: ALL containers (Docker or Podman), background web servers, microservices, and Linux tooling MUST be executed inside the **WSL (Windows Subsystem for Linux)** environment, NEVER natively on Windows host command prompt or PowerShell.
