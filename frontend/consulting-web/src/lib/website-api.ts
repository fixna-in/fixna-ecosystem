import { z } from "zod";
import { apiClient } from "./api-client";

export const ProfileSchema = z.object({
  id: z.string().nullable().optional(),
  displayName: z.string().nullable().optional(),
  tagline: z.string().nullable().optional(),
  bio: z.string().nullable().optional(),
  websiteUrl: z.string().nullable().optional(),
  logoUrl: z.string().nullable().optional(),
  publicSlug: z.string().nullable().optional(),
  published: z.boolean(),
  createdAt: z.string().nullable().optional(),
  updatedAt: z.string().nullable().optional(),
});

export type Profile = z.infer<typeof ProfileSchema>;

export const ProfileInputSchema = z.object({
  displayName: z.string().max(200).optional(),
  tagline: z.string().max(500).optional(),
  bio: z.string().optional(),
  websiteUrl: z.string().max(1000).optional(),
  logoUrl: z.string().max(1000).optional(),
  publicSlug: z.string().max(100).optional(),
  published: z.boolean().optional(),
});

export type ProfileInput = z.infer<typeof ProfileInputSchema>;

export const TestimonialSchema = z.object({
  id: z.string(),
  authorName: z.string(),
  authorTitle: z.string().nullable().optional(),
  quote: z.string(),
  published: z.boolean(),
  sortOrder: z.number(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Testimonial = z.infer<typeof TestimonialSchema>;

export const TestimonialInputSchema = z.object({
  authorName: z.string().min(1).max(200),
  authorTitle: z.string().max(200).optional(),
  quote: z.string().min(1),
  published: z.boolean(),
  sortOrder: z.number().int(),
});

export type TestimonialInput = z.infer<typeof TestimonialInputSchema>;

export async function getProfile(): Promise<Profile> {
  const { data } = await apiClient.get("/profile");
  return ProfileSchema.parse(data);
}

export async function updateProfile(input: ProfileInput): Promise<Profile> {
  const { data } = await apiClient.put("/profile", input);
  return ProfileSchema.parse(data);
}

export async function listTestimonials(): Promise<Testimonial[]> {
  const { data } = await apiClient.get("/testimonials");
  return z.array(TestimonialSchema).parse(data);
}

export async function createTestimonial(input: TestimonialInput): Promise<Testimonial> {
  const { data } = await apiClient.post("/testimonials", input);
  return TestimonialSchema.parse(data);
}

export async function updateTestimonial(id: string, input: TestimonialInput): Promise<Testimonial> {
  const { data } = await apiClient.put(`/testimonials/${id}`, input);
  return TestimonialSchema.parse(data);
}

export async function deleteTestimonial(id: string): Promise<void> {
  await apiClient.delete(`/testimonials/${id}`);
}
