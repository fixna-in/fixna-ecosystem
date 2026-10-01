# Build and Deployment Model

## Independent frontend builds

| Product | Frontend | Suggested deployment | API | Database |
|---|---|---|---|---|
| LocalBoost | frontend/localboost-web | app.fixna.in | backend/localboost-api | isolated LocalBoost DB |
| Consulting | frontend/consulting-web | consulting.fixna.in | backend/consulting-api | isolated Consulting DB |
| Hospitality | frontend/hospitality-web | hospitality.fixna.in | backend/hospitality-api | isolated Hospitality DB |

Each product can be deployed without rebuilding or redeploying another product.

Shared TypeScript can be extracted later if duplication appears across frontends; there is no shared `packages/` workspace today.

## LocalBoost safety rule
The existing live LocalBoost application must not be moved or refactored merely to introduce the ecosystem structure. The ecosystem structure should be adopted around it incrementally.
