import { z } from "zod";
import { apiClient } from "./api-client";

export const WebsiteAuditCheckSchema = z.object({
  id: z.string(),
  checkCode: z.string(),
  severity: z.string(),
  score: z.number(),
  fixHint: z.string().nullable().optional(),
  evidence: z.record(z.string(), z.string()).optional().default({}),
});

export type WebsiteAuditCheck = z.infer<typeof WebsiteAuditCheckSchema>;

export const WebsiteAuditSchema = z.object({
  id: z.string(),
  inputUrl: z.string(),
  profileId: z.string().nullable().optional(),
  status: z.string(),
  overallScore: z.number(),
  summary: z.string(),
  createdAt: z.string(),
  checks: z.array(WebsiteAuditCheckSchema).optional().default([]),
});

export type WebsiteAudit = z.infer<typeof WebsiteAuditSchema>;

export const WebsiteAuditJobSchema = z.object({
  jobId: z.string(),
  status: z.string(),
  attempt: z.number(),
  maxAttempts: z.number(),
  progress: z.object({
    pagesFetched: z.number(),
    budgetPages: z.number(),
    budgetSeconds: z.number(),
  }),
  failureCode: z.string().nullable().optional(),
  auditId: z.string().nullable().optional(),
  linksSelf: z.string(),
  linksAudit: z.string().nullable().optional(),
});

export type WebsiteAuditJob = z.infer<typeof WebsiteAuditJobSchema>;

export const WebsiteAuditInputSchema = z.object({
  url: z.string().url().max(2000),
});

export type WebsiteAuditInput = z.infer<typeof WebsiteAuditInputSchema>;

export const WebsiteAuditFixesSchema = z.object({
  disclaimer: z.string(),
  suggestion: z.object({
    url: z.string(),
    priorities: z.array(z.string()),
  }),
});

export type WebsiteAuditFixes = z.infer<typeof WebsiteAuditFixesSchema>;

export async function runWebsiteAudit(input: WebsiteAuditInput): Promise<WebsiteAudit> {
  // Audits fetch the target page plus robots.txt and sitemap.xml with hard timeouts.
  const { data } = await apiClient.post("/website-audits", input, { timeout: 45000 });
  return WebsiteAuditSchema.parse(data);
}

export async function listWebsiteAudits(): Promise<WebsiteAudit[]> {
  const { data } = await apiClient.get("/website-audits");
  return z.array(WebsiteAuditSchema).parse(data);
}

export async function getWebsiteAudit(id: string): Promise<WebsiteAudit> {
  const { data } = await apiClient.get(`/website-audits/${id}`);
  return WebsiteAuditSchema.parse(data);
}

export async function suggestAuditFixes(id: string): Promise<WebsiteAuditFixes> {
  const { data } = await apiClient.post(`/website-audits/${id}/fixes`);
  return WebsiteAuditFixesSchema.parse(data);
}

export async function submitWebsiteAuditJob(input: WebsiteAuditInput): Promise<WebsiteAuditJob> {
  const { data } = await apiClient.post("/website-audits/jobs", {
    url: input.url,
    crawl: true,
  });
  return WebsiteAuditJobSchema.parse(data);
}

export async function getWebsiteAuditJob(id: string): Promise<WebsiteAuditJob> {
  const { data } = await apiClient.get(`/website-audits/jobs/${id}`);
  return WebsiteAuditJobSchema.parse(data);
}

export async function cancelWebsiteAuditJob(id: string): Promise<WebsiteAuditJob> {
  const { data } = await apiClient.post(`/website-audits/jobs/${id}/cancel`);
  return WebsiteAuditJobSchema.parse(data);
}

export async function listWebsiteAuditJobs(): Promise<WebsiteAuditJob[]> {
  const { data } = await apiClient.get("/website-audits/jobs");
  return z.array(WebsiteAuditJobSchema).parse(data);
}

export function websiteAuditReportUrl(id: string): string {
  const base = apiClient.defaults.baseURL ?? "";
  return `${base}/website-audits/${id}/report`;
}
