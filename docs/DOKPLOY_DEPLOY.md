# Dokploy Deployment Handoff Guide

This guide details the deployment specifications for all backend services in the **Fixna Ecosystem** running on Dokploy (Hostinger VPS) with PostgreSQL 18 (`fixnainfra-postgresadmin-qfasaa`).

---

## **Global Infrastructure Architecture**
- **Database Server**: `fixnainfra-postgresadmin-qfasaa:5432` (Internal Docker network only)
- **Reverse Proxy**: Traefik (manages public HTTPS routing to container port 80)
- **Container Port**: `80` (All backend services listen on port 80 inside their container)
- **Network Requirement**: Every application container must join the same internal Docker network as the PostgreSQL service.

---

## **Service Configurations**

### **1. Consulting API (`consulting-api`)**
- **Image Name**: `ghcr.io/fixna-in/consulting-api`
- **Tags**: `prod` (for main), `dev` (for dev), `build-<RUN_NUMBER>` (incremental build version), or `<SHA>`
- **Container Port**: `80`
- **Domain**: 
  - Production: `api.consulting.fixna.in`
  - Development: `dev.api.consulting.fixna.in`
- **Health Path**: `/api/v1/health/ready`
- **Database URL Format**: 
  `postgresql://consulting_user:<PASSWORD>@fixnainfra-postgresadmin-qfasaa:5432/consulting_db`
- **Runtime Variables**:
  - `SPRING_PROFILES_ACTIVE` (`prod` or `dev`)
  - `POSTGRES_PASSWORD`
  - `FIXNA_JWT_SECRET`
  - `FIXNA_CORS_ALLOWED_ORIGINS`
- **Build-Time Variables**: None required

---

### **2. LocalBoost API (`localboost-api`)**
- **Image Name**: `ghcr.io/fixna-in/localboost-api`
- **Tags**: `prod`, `dev`, `build-<RUN_NUMBER>`, or `<SHA>`
- **Container Port**: `80`
- **Domain**: 
  - Production: `api.localboost.fixna.in`
  - Development: `dev.api.localboost.fixna.in`
- **Health Path**: `/api/v1/health/readiness`
- **Database URL Format**: 
  `postgresql://localboost_user:<PASSWORD>@fixnainfra-postgresadmin-qfasaa:5432/localboost_db`
- **Runtime Variables**:
  - `SPRING_PROFILES_ACTIVE` (`prod` or `dev`)
  - `POSTGRES_PASSWORD`
  - `FIXNA_JWT_SECRET`
  - `FIXNA_CORS_ALLOWED_ORIGINS`
  - `REDIS_URL` (optional)
- **Build-Time Variables**: None required

---

### **3. Hospitality API (`hospitality-api`)**
- **Image Name**: `ghcr.io/fixna-in/hospitality-api`
- **Tags**: `prod`, `dev`, `build-<RUN_NUMBER>`, or `<SHA>`
- **Container Port**: `80`
- **Container Port**: `80`
- **Domain**: 
  - Production: `api.hospitality.fixna.in`
  - Development: `dev.api.hospitality.fixna.in`
- **Health Path**: `/api/v1/health/readiness`
- **Database URL Format**: 
  `postgresql://hospitality_user:<PASSWORD>@fixnainfra-postgresadmin-qfasaa:5432/hospitality_db`
- **Runtime Variables**:
  - `SPRING_PROFILES_ACTIVE` (`prod` or `dev`)
  - `POSTGRES_PASSWORD`
  - `FIXNA_JWT_SECRET`
  - `FIXNA_CORS_ALLOWED_ORIGINS`
- **Build-Time Variables**: None required

---

### **4. Website Audit API (`website-audit-api`)**
- **Image Name**: `ghcr.io/fixna-in/website-audit-api`
- **Tags**: `prod`, `dev`, `build-<RUN_NUMBER>`, or `<SHA>`
- **Container Port**: `80`
- **Domain**: 
  - Production: `api.website-audit.fixna.in`
  - Development: `dev.api.website-audit.fixna.in`
- **Health Path**: `/api/v1/health/ready`
- **Database URL Format**: 
  `postgresql://website_audit_user:<PASSWORD>@fixnainfra-postgresadmin-qfasaa:5432/website_audit_db`
- **Runtime Variables**:
  - `SPRING_PROFILES_ACTIVE` (`prod` or `dev`)
  - `POSTGRES_PASSWORD`
  - `FIXNA_JWT_SECRET`
  - `FIXNA_CORS_ALLOWED_ORIGINS`
- **Build-Time Variables**: None required

---

### **5. Invoice Assistant API (`invoice-assistant-api`)**
- **Image Name**: `ghcr.io/fixna-in/invoice-assistant-api`
- **Tags**: `prod`, `dev`, `build-<RUN_NUMBER>`, or `<SHA>`
- **Container Port**: `80`
- **Domain**: 
  - Production: `api.invoice-assistant.fixna.in`
  - Development: `dev.api.invoice-assistant.fixna.in`
- **Health Path**: `/api/v1/health/ready`
- **Database URL Format**: 
  `postgresql://invoice_assistant_user:<PASSWORD>@fixnainfra-postgresadmin-qfasaa:5432/invoice_assistant_db`
- **Runtime Variables**:
  - `SPRING_PROFILES_ACTIVE` (`prod` or `dev`)
  - `POSTGRES_PASSWORD`
  - `FIXNA_JWT_SECRET`
  - `FIXNA_CORS_ALLOWED_ORIGINS`
- **Build-Time Variables**: None required
