# Website Audit — Product Overview

Standalone SaaS product within the Fixna ecosystem. Identifies technical and
on‑page website issues, shows evidence, and provides a prioritized remediation
plan. Independently deployable (own API, own database, own frontend).

## Status: ✅ LIVE — https://websiteaudit.fixna.in

## Boundaries

- API: `backend/website-audit-api` (Java 21 / Spring Boot 3.5.6 / Maven), port **8082**
- Web: `frontend/website-audit-web` (Next.js 16 / React 19 / TypeScript), port **3003**
- Database: PostgreSQL `website_audit` (Flyway `V1–V3`), local port **5434**
- Deployment: `render-website-audit.yaml` (API on Render) + `frontend/website-audit-web` on Vercel

Do **not** share migrations or databases with Consulting, LocalBoost, or the
Invoice Assistant product.

## What is implemented (MVP)

- JWT auth (register / login / refresh), `/me`, tenant + membership isolation, RBAC.
- Bounded crawl with SSRF + DNS‑rebinding defenses (ADR‑009/010): http/https only,
  ports 80/443, resolve‑once pinned connect, no redirects, 512 KB body cap.
- Deterministic checks with severity, score, evidence and fix hints.
- Async audit jobs (202 submit + poll + cancel) with bounded worker.
- Self‑contained HTML report (CSP `default-src 'none'`).
- Advisory‑only AI fixes (`POST /website-audits/{id}/fixes`) — never authoritative.
- Auth‑surface rate limiting; health probes at `/api/v1/health` and `/ready`.

## Frontend routes

- `/` — dashboard: submit an audit job, poll progress, list recent audits.
- `/login` — sign in / create a workspace.
- `/audits/[id]` — findings, overall score, report download, AI fix suggestions.

## Not implemented (by design)

- Public marketing/pricing page, `/features`, `/signup` (auth is combined into `/login`).
- Continuous monitoring, automatic fixes, JS rendering/screenshots.
- Subscription/usage billing endpoints.

See `docs/specs/website-audit-*` for the deeper product/API/DB/threat‑model specs.
