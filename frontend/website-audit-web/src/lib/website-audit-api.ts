import apiClient from './api-client';

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8082/api';

export interface AuditCheck {
  id: string;
  checkCode: string;
  severity: string;
  score: number;
  fixHint: string | null;
  evidence: Record<string, string> | null;
}

export interface Audit {
  id: string;
  inputUrl: string;
  status: string;
  overallScore: number;
  summary: string | null;
  createdAt: string;
  checks: AuditCheck[];
}

export interface AuditJob {
  jobId: string;
  status: string;
  attempt: number;
  maxAttempts: number;
  progress: { pagesFetched: number; budgetPages: number; budgetSeconds: number };
  failureCode: string | null;
  auditId: string | null;
  linksSelf: string;
  linksAudit: string | null;
}

export interface FixSuggestionResponse {
  disclaimer: string;
  suggestion: { url: string; priorities: string[] };
}

export async function listAudits(): Promise<Audit[]> {
  const { data } = await apiClient.get('/v1/website-audits');
  return data;
}

export async function getAudit(id: string): Promise<Audit> {
  const { data } = await apiClient.get(`/v1/website-audits/${id}`);
  return data;
}

export async function submitJob(url: string, crawl: boolean): Promise<AuditJob> {
  const { data } = await apiClient.post('/v1/website-audits/jobs', { url, crawl });
  return data;
}

export async function getJob(jobId: string): Promise<AuditJob> {
  const { data } = await apiClient.get(`/v1/website-audits/jobs/${jobId}`);
  return data;
}

export async function cancelJob(jobId: string): Promise<AuditJob> {
  const { data } = await apiClient.post(`/v1/website-audits/jobs/${jobId}/cancel`);
  return data;
}

export async function suggestFixes(id: string): Promise<FixSuggestionResponse> {
  const { data } = await apiClient.post(`/v1/website-audits/${id}/fixes`);
  return data;
}

/** Report requires a Authorization header, so fetch it as a blob and open via object URL. */
export async function fetchReportBlob(id: string): Promise<Blob> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const res = await fetch(`${API_BASE}/v1/website-audits/${id}/report`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`Report download failed (${res.status})`);
  }
  return res.blob();
}

export function terminal(job: AuditJob): boolean {
  return ['COMPLETED', 'FAILED', 'CANCELLED'].includes(job.status);
}
