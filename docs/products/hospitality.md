# Hospitality

**Status: ✅ LIVE — https://hospitality.fixna.in**

Configurable technology platform for hotel and hospitality operations. Independently deployable (own API, own database, own frontend).

## Boundaries

- API: `backend/hospitality-api` (Java 21 / Spring Boot 3.5.6 / Maven), port **8084**
- Web: `frontend/hospitality-web` (Next.js 16 / React 19 / TypeScript), port **3005**
- Database: PostgreSQL `hospitality` (Flyway), local port **5436**
- Deployment: `render-hospitality.yaml` (API on Render) + `frontend/hospitality-web` on Vercel

Do **not** share migrations or databases with Consulting, LocalBoost, Website Audit, or Invoice Assistant products.

## What is implemented (MVP)

- JWT auth (register / login / refresh), `/me`, tenant + membership isolation, RBAC.
- Operations management, guest experiences, service workflows, business configuration.

## Frontend routes

- `/` — dashboard
- `/login` — sign in / create a workspace
- Operations, guest management, service workflows, configuration

See `docs/specs/` for deeper product/API/DB/threat‑model specs when available.
