# Invoice Assistant — Product Overview

Standalone SaaS product within the Fixna ecosystem. Converts invoices and
receipts into structured, reviewable records and exports. Independently
deployable (own API, own database, own frontend).

## Boundaries

- API: `backend/invoice-assistant-api` (Java 21 / Spring Boot 3.5.6 / Maven), port **8083**
- Web: `frontend/invoice-assistant-web` (Next.js 16 / React 19 / TypeScript), port **3004**
- Database: PostgreSQL `invoice_assistant` (Flyway `V1–V6`), local port **5435**
- Deployment: `render-invoice-assistant.yaml` (API on Render) + `frontend/invoice-assistant-web` on Vercel

Do **not** share migrations or databases with Consulting, LocalBoost, or the
Website Audit product.

## What is implemented (MVP)

- JWT auth (register / login / refresh), `/me`, tenant + membership isolation, RBAC.
- Private PDF intake (ADR‑011): PDF‑only, ≤10 MB, magic‑byte check, sha256, private
  `FileStorage`; bytes are only served back through the authenticated
  `/content` endpoint — never a public URL.
- Async extraction job (native PDF text → offline OCR fallback) with polling.
- Field‑level provenance and deterministic confidence; money via exact `BigDecimal`.
- Confidence gates: confirm low‑confidence fields before approval.
- Review workflow: confirm → approve / reject → post; each transition audited.
- CSV / JSON export; cross‑tenant search; retention purge job; monthly quotas.

## Frontend routes

- `/` — dashboard: upload a PDF, list documents, delete.
- `/login` — sign in / create a workspace.
- `/documents/[documentId]` — extract, review fields with confidence badges,
  edit/confirm, approve/reject/post, export CSV/JSON, download the original.

## Not implemented (by design)

- Public marketing/pricing page, `/features`, `/signup` (auth is combined into `/login`).
- Cloud OCR (offline engine only), accounting integrations, tax filing/payments.
- Subscription/usage billing endpoints.

See `docs/specs/invoice-assistant-*` for the deeper product/API/DB/threat‑model specs.
