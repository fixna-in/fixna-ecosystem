# Fixna Ecosystem — Application Handover Document (Claude / AI Agent Guide)

Welcome! This document provides a complete architectural and operational handover for the **Fixna Ecosystem** monorepo. It is specifically designed to orient AI coding agents (such as Claude, Cursor, or Cline) on standards, conventions, and deployment rules across all independent products.

---

## 1. Executive Summary
The Fixna Ecosystem is a **multi-product monorepo** hosting independent business applications. Each product deploys as its own isolated stack:
- **API**: Spring Boot 3.5.6 (Java 21) listening on **Port 80** in production containers.
- **Web**: Next.js 16+ (React 19, TypeScript) deployed on Vercel (or standby Docker images).
- **Database**: Independent PostgreSQL 17/18 instance with dedicated user roles (`<app>_user` owning `<app>_db`) and Flyway migration history.
- **Shared Infrastructure**: Only `backend/libs/fixna-platform-common` (logging, security headers, CORS, error handling).

---

## 2. Products & Service Inventory

| Product | API Service | Web App | Database Name | App Version | Health Endpoint |
| :--- | :--- | :--- | :--- | :---: | :--- |
| **Consulting** | `backend/consulting-api` | `frontend/consulting-web` | `consulting_db` | `1.2.0` | `/api/v1/health/ready` |
| **LocalBoost** | `backend/localboost-api` | `frontend/localboost-web` | `localboost_db` | `1.1.0` | `/api/v1/health/readiness` |
| **Hospitality** | `backend/hospitality-api` | `frontend/hospitality-web` | `hospitality_db` | `1.1.0` | `/api/v1/health/readiness` |
| **Website Audit** | `backend/website-audit-api` | `frontend/website-audit-web` | `website_audit_db` | `1.1.0` | `/api/v1/health` |
| **Invoice Assistant**| `backend/invoice-assistant-api`| `frontend/invoice-assistant-web`| `invoice_assistant_db`| `1.1.0` | `/api/v1/health` |
| **Shared Platform**| `backend/libs/fixna-platform-common` | — | — | `1.0.0` | — |

---

## 3. Core Architecture Rules & Non-Negotiables

1. **Strict Product Isolation**: 
   - **Never** merge Flyway migrations or share database schemas between products. Each product owns its migrations under `backend/{product}-api/src/main/resources/db/migration/`.
2. **Multi-Tenant Security**: 
   - Preserve multi-tenant isolation. **Never** trust a tenant ID supplied directly by the client; always resolve it from the authenticated JWT security context.
3. **Configuration & Profiles**:
   - `application.yml` contains **only common shared configurations** (Actuator, OpenAPI, Logging).
   - Environment-specific properties reside in `application-local.yml`, `application-dev.yml`, and `application-prod.yml`.
   - Profiles are activated via system environment variables (`SPRING_PROFILES_ACTIVE=dev` or `prod`).
4. **Environment Variables & Secrets**:
   - All secrets (passwords, JWT keys) are sourced exclusively from environment variables (`POSTGRES_PASSWORD`, `FIXNA_JWT_SECRET`).
   - Centralized environment reference placeholders are located in the **`env/`** folder (e.g., `env/consulting-api.env`).
5. **Port & Routing**:
   - All containers listen on internal **Port 80** (`PORT=80`). Public TLS termination and routing are handled by Dokploy's Traefik reverse proxy.
6. **Database Seeding**:
   - Default administrative user (`fixnainfo@gmail.com`) is provisioned via versioned Flyway migration scripts (`ON CONFLICT DO NOTHING`) running on startup in `dev` and `prod`.

---

## 4. Build & Test Commands

### **Java Backend (Maven)**
```bash
# Build the entire ecosystem reactor
mvn -f pom.xml clean compile

# Run unit tests across all APIs
npm run test:all-apis  # or mvn test from root

# Build specific API jar with build-info versioning
mvn -f backend/consulting-api/pom.xml clean package
```

### **Docker Container Build**
```bash
# Build monorepo Docker image (context is repo root to include fixna-platform-common)
docker build -f backend/consulting-api/Dockerfile -t fixna-consulting-api .
```

---

## 5. Health API & Versioning Contract
The public health endpoint (`GET /api/v1/health`) returns an aggregated JSON payload containing the application-specific version (read from Maven `build-info`), runtime environment, service name, and component health status:

```json
{
  "status": "UP",
  "version": "1.2.0",
  "deployedAt": "2026-11-19T00:00:00Z",
  "environment": "prod",
  "service": "consulting-api",
  "timestamp": "2026-11-19T00:35:00Z",
  "components": {
    "db": { "status": "UP", "details": {} },
    "diskSpace": { "status": "UP", "details": {} }
  }
}
```
