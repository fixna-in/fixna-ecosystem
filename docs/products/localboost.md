# LocalBoost

Marketing and lead-generation platform for local businesses.

## Paths

| Layer | Location |
|-------|----------|
| API | `backend/localboost-api` |
| Web | `frontend/localboost-web` |
| Deploy | `render-localboost.yaml` |
| AI prompts/schemas | `ai/localboost/` |
| Product docs | [`docs/products/localboost/`](./localboost/) |

## Production (current)

| Surface | URL |
|---------|-----|
| Web | https://app.fixna.in |
| API | https://api.fixna.in/api |

Production currently deploys from standalone repo `fixna-localboost`. Ecosystem monorepo copy is ready for cutover.

## Local run

```bash
docker compose up -d
npm run bootstrap:maven
mvn -f backend/localboost-api/pom.xml spring-boot:run    # :8080
npm --prefix frontend/localboost-web run dev             # :3000
```

Env: copy `frontend/localboost-web/.env.example` → `.env.local`

## Build & test

```bash
npm run test:localboost-api
npm run build:localboost
```

## Domain ownership

Campaigns, businesses, leads, audiences, creatives, subscription billing, platform ad adapters, LocalBoost AI.

See [architecture/product-boundaries.md](../architecture/product-boundaries.md).
