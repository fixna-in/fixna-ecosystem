# Product scaffolding

## Monorepo layout

```text
fixna-ecosystem/
├── backend/                         # All Java code
│   ├── libs/fixna-platform-common/  # Shared logging, security, web errors
│   ├── {product}-api/               # Spring Boot API per product
│   └── ...
├── frontend/                        # All Next.js apps
│   └── {product}-web/
├── docs/                            # Docs for both
├── scripts/                         # Dev scripts
├── tools/                           # SQL, utilities
└── docker-compose.yml               # Local infra
```

## API service (`backend/{product}-api`)

```bash
npm run bootstrap:maven   # once — installs fixna-platform-common:1.0.0
mvn -f backend/{product}-api/pom.xml spring-boot:run
docker build -f backend/{product}-api/Dockerfile -t fixna-{product}-api .
```

## Web app (`frontend/{product}-web`)

```bash
npm --prefix frontend/{product}-web run dev
npm run build:{product}
```

## Render blueprint (`render-{product}.yaml`)

```yaml
dockerfilePath: ./backend/{product}-api/Dockerfile
dockerContext: .
buildFilter:
  paths:
    - backend/{product}-api/**
    - backend/libs/fixna-platform-common/**
    - pom.xml
```

Web: Vercel root directory `frontend/{product}-web`

## Shared Java library

`backend/libs/fixna-platform-common` — CORS, security headers, API error envelope, logging MDC.

Each product registers `TenantScopeProvider` to wire its own tenant context.
