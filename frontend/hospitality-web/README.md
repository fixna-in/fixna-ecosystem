# hospitality-web

Hospitality frontend — Next.js 16 (App Router), React 19, TypeScript.

## Development

```bash
npm run dev          # runs on http://localhost:3002
npm run build        # production build
npm run lint         # lint
npm run test         # unit tests
```

## Environment

Copy `.env.example` to `.env.local` and adjust:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8084/api
NEXT_PUBLIC_APP_ENV=local
```

## Deployment

Vercel: root directory `frontend/hospitality-web`, framework preset Next.js.