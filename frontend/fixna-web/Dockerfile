# fixna-web — multi-stage standalone build.
# Build context is the REPO ROOT:
#   docker build -f frontend/fixna-web/Dockerfile -t fixna-web .
# Run:   docker run --rm -p 3000:3000 fixna-web   (PORT env overrides 3000)

ARG NEXT_PUBLIC_SITE_URL

FROM node:22-alpine AS builder
WORKDIR /app

ARG NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}

# Copy package manifests across the monorepo first so npm workspaces can link correctly.
COPY package.json package-lock.json ./
COPY packages/fixna-brand/package.json ./packages/fixna-brand/package.json
COPY packages/fixna-design-system/package.json ./packages/fixna-design-system/package.json
COPY frontend/fixna-web/package.json ./frontend/fixna-web/package.json

RUN npm ci

# Copy full source code for packages and this app.
COPY packages/fixna-brand ./packages/fixna-brand
COPY packages/fixna-design-system ./packages/fixna-design-system
COPY frontend/fixna-web ./frontend/fixna-web

RUN npm run build --workspace frontend/fixna-web

FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

RUN addgroup -S -g 1001 nodejs \
 && adduser -S -u 1001 -G nodejs nextjs

# Standalone output: server at frontend/fixna-web/server.js with traced deps.
COPY --from=builder --chown=nextjs:nodejs /app/frontend/fixna-web/.next/standalone ./
# Static assets are served from .next/static inside the standalone tree.
COPY --from=builder --chown=nextjs:nodejs /app/frontend/fixna-web/.next/static ./frontend/fixna-web/.next/static

USER nextjs

EXPOSE 3000

CMD ["node", "frontend/fixna-web/server.js"]

