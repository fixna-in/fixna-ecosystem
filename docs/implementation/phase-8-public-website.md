# Phase 8 — Public Website

**Status:** Complete  
**Version:** 0.8.0  
**Date:** 2026-09-28

## Goal

Publish a tenant-scoped marketing site with profile, services, and testimonials — plus workspace tools to manage content.

## Backend

### Migration `V8__public_website.sql`
- `consulting_profiles.public_slug` — unique URL slug (nullable)
- `consulting_profiles.is_published` — publish toggle
- `testimonials` table with publish flag and sort order

### Authenticated management
| Endpoint | Permission | Notes |
|----------|------------|-------|
| `PUT /api/v1/profile` | WEBSITE_MANAGE | Profile + slug + publish |
| `GET/POST/PUT/DELETE /api/v1/testimonials` | TESTIMONIAL_MANAGE | CRUD |

### Public API (no auth)
| Endpoint | Notes |
|----------|-------|
| `GET /api/v1/public/sites/{slug}` | Published profile, active services, published testimonials |

Slug rules: lowercase letters, numbers, hyphens. Publishing requires a slug.

## Frontend

- `/site/[slug]` — public marketing page (server-rendered, no auth)
- `/website` — consultant admin for profile, publish settings, testimonials
- `src/lib/public-api.ts`, `src/lib/website-api.ts`

## Tests

- `PublicWebsiteServiceTest` — unpublished hidden, published content assembly
- `TestimonialApplicationServiceTest` — RBAC + tenant isolation

## Next: Phase 9 — AI Assistance
