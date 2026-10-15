# invoice-assistant-web

Invoice Assistant frontend — Next.js 16 (App Router), React 19, TypeScript.

## Development

```bash
npm run dev          # runs on http://localhost:3004
npm run build        # production build
npm run typecheck    # tsc --noEmit
npm run test         # vitest run
npm run lint         # lint
```

## Environment

Copy `.env.example` to `.env.local` and adjust:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8083/api
NEXT_PUBLIC_APP_ENV=local
```

## Deployment

Vercel: root directory `frontend/invoice-assistant-web`, framework preset Next.js.
