# Hospitality

**Status:** Future placeholder — no implementation.

## Planned paths

| Layer | Location |
|-------|----------|
| API | `backend/hospitality-api` (README only) |
| Web | `frontend/hospitality-web` (README only) |
| Deploy | `render-hospitality.yaml` (not created) |

When implemented, follow [`docs/scaffolding.md`](../scaffolding.md):

- Separate PostgreSQL database and Flyway history
- No dependencies on LocalBoost or Consulting domain code
- Independent Render + Vercel deploy units

Do not add Hospitality to production builds or CI until scoped.
