import { z } from "zod";

function resolvePublicBaseUrl(): string {
  const raw =
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    "http://localhost:8081/api/v1";
  const trimmed = raw.replace(/\/+$/, "");
  return trimmed.endsWith("/v1") ? trimmed : `${trimmed}/v1`;
}

const PublicProfileSchema = z.object({
  displayName: z.string().nullable().optional(),
  tagline: z.string().nullable().optional(),
  bio: z.string().nullable().optional(),
  websiteUrl: z.string().nullable().optional(),
  logoUrl: z.string().nullable().optional(),
  publicSlug: z.string().nullable().optional(),
});

const PublicServiceSchema = z.object({
  name: z.string(),
  description: z.string().nullable().optional(),
  priceAmount: z.union([z.number(), z.string()]).nullable().optional(),
  priceCurrency: z.string().nullable().optional(),
  durationMinutes: z.number().nullable().optional(),
  sortOrder: z.number(),
});

const PublicTestimonialSchema = z.object({
  authorName: z.string(),
  authorTitle: z.string().nullable().optional(),
  quote: z.string(),
  sortOrder: z.number(),
});

export const PublicWebsiteSchema = z.object({
  profile: PublicProfileSchema,
  services: z.array(PublicServiceSchema),
  testimonials: z.array(PublicTestimonialSchema),
});

export type PublicWebsite = z.infer<typeof PublicWebsiteSchema>;
export type PublicService = z.infer<typeof PublicServiceSchema>;
export type PublicTestimonial = z.infer<typeof PublicTestimonialSchema>;

export async function fetchPublicSite(slug: string): Promise<PublicWebsite | null> {
  const response = await fetch(`${resolvePublicBaseUrl()}/public/sites/${encodeURIComponent(slug)}`, {
    headers: { Accept: "application/json" },
    next: { revalidate: 60 },
  });
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new Error(`Failed to load public site (${response.status})`);
  }
  const data = await response.json();
  return PublicWebsiteSchema.parse(data);
}
