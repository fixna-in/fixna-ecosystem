# hospitality-web

Hospitality frontend — Next.js 16 (App Router), React 19, TypeScript.

## Routes

| Route | Access | Purpose |
|-------|--------|---------|
| `/` | Public | Marketing home; links to sign in / register |
| `/login` | Public | Email + password sign-in (`POST /api/v1/auth/login`) |
| `/register` | Public | Account + workspace signup (`POST /api/v1/auth/register`) |
| `/dashboard` | Authenticated | Workspace overview; redirects to `/login` when signed out |

Auth state is provided by `AuthProvider` (`src/lib/auth-context.tsx`), mounted in
`src/app/providers.tsx`. After login/register the client calls `GET /api/v1/me`
to restore the session on reload.

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