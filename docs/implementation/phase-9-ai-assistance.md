# Phase 9 — AI Assistance

**Status:** Complete  
**Version:** 0.9.0  
**Date:** 2026-09-28

## Goal

Provide advisory AI suggestions for common consulting workflows. No autonomous actions — humans review and apply suggestions manually.

## Backend

### Migration `V9__ai_assistance.sql`
- `ai_usage_logs` — tenant/user scoped usage audit trail

### Provider abstraction (`ai/`)
- `AiProvider` interface
- `MockAiProvider` — deterministic templates (default, no external API)
- Features: `PROPOSAL_DRAFT`, `MEETING_AGENDA`, `CLIENT_SUMMARY`, `WEBSITE_COPY`

### Permission
- `AI_USE` — granted to `CONSULTANT_ADMIN` and `CONSULTANT`

### API (`/api/v1/ai/*`)
| Endpoint | Feature |
|----------|---------|
| `POST /proposals/suggest` | Proposal draft + line items |
| `POST /meetings/suggest-agenda` | Meeting agenda |
| `POST /clients/{id}/summarize` | Client summary + next steps |
| `POST /website/suggest-copy` | Tagline + bio suggestions |

Each request logs usage, emits `ai.suggestion.generated` audit event, and returns an advisory disclaimer.

## Frontend

- `/ai` — tabbed assistant UI (proposal, meeting, client, website)
- `src/lib/ai-api.ts`

## Tests

- `MockAiProviderTest` — deterministic output
- `AiAssistantApplicationServiceTest` — RBAC, usage logging, tenant isolation

## Next: Phase 10 — Hardening
