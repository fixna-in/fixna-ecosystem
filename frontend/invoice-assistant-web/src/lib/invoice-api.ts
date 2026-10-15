import { z } from 'zod';
import apiClient from './api-client';

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8083/api';

// --- Auth -----------------------------------------------------------------

export const AuthResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  userId: z.string(),
  tenantId: z.string(),
  role: z.string(),
});
export type AuthResponse = z.infer<typeof AuthResponseSchema>;

export const RegisterSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8).max(200),
  firstName: z.string().max(100).optional(),
  lastName: z.string().max(100).optional(),
});
export type RegisterInput = z.infer<typeof RegisterSchema>;

export interface Me {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
}

export async function login(input: { email: string; password: string }): Promise<AuthResponse> {
  const { data } = await apiClient.post('/v1/auth/login', input);
  return AuthResponseSchema.parse(data);
}

export async function register(input: RegisterInput): Promise<AuthResponse> {
  const { data } = await apiClient.post('/v1/auth/register', input);
  return AuthResponseSchema.parse(data);
}

export async function me(): Promise<Me> {
  const { data } = await apiClient.get('/v1/me');
  const parsed = z
    .object({
      id: z.string(),
      email: z.string(),
      firstName: z.string().nullable().nullish(),
      lastName: z.string().nullable().nullish(),
    })
    .parse(data);
  return {
    id: parsed.id,
    email: parsed.email,
    firstName: parsed.firstName ?? null,
    lastName: parsed.lastName ?? null,
  };
}

// --- Documents ------------------------------------------------------------

export const InvoiceDocumentSchema = z.object({
  documentId: z.string(),
  invoiceRecordId: z.string().nullable().optional(),
  fileName: z.string(),
  sizeBytes: z.number(),
  status: z.string(),
  createdAt: z.string(),
});
export type InvoiceDocument = z.infer<typeof InvoiceDocumentSchema>;

export const DocumentPageSchema = z.object({
  content: z.array(InvoiceDocumentSchema).optional().default([]),
  page: z.number().optional().default(0),
  size: z.number().optional().default(50),
  totalElements: z.number().optional().default(0),
  totalPages: z.number().optional().default(0),
});
export type DocumentPage = z.infer<typeof DocumentPageSchema>;

export const InvoiceDocumentJobSchema = z.object({
  jobId: z.string(),
  documentId: z.string(),
  status: z.string(),
  errorCode: z.string().nullable().optional(),
  errorMessage: z.string().nullable().optional(),
  createdAt: z.string(),
});
export type InvoiceDocumentJob = z.infer<typeof InvoiceDocumentJobSchema>;

export const InvoiceFieldSchema = z.object({
  id: z.string(),
  fieldKey: z.string(),
  fieldValue: z.string().nullable().optional(),
  moneyAmount: z.union([z.string(), z.number()]).nullable().optional(),
  currency: z.string().nullable().optional(),
  confidence: z.union([z.string(), z.number()]),
  source: z.string(),
  page: z.number().nullable().optional(),
  bbox: z.record(z.string(), z.any()).nullable().optional(),
  rawSnippet: z.string().nullable().optional(),
});
export type InvoiceField = z.infer<typeof InvoiceFieldSchema>;

export const InvoiceExtractionSchema = z.object({
  extractionId: z.string(),
  documentId: z.string(),
  status: z.string(),
  methodNativePages: z.number(),
  methodOcrPages: z.number(),
  extractorVersion: z.string().nullable().optional(),
  postedInvoiceRecordId: z.string().nullable().optional(),
  createdAt: z.string(),
  fields: z.array(InvoiceFieldSchema).optional().default([]),
});
export type InvoiceExtraction = z.infer<typeof InvoiceExtractionSchema>;

export async function uploadDocument(file: File): Promise<InvoiceDocument> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await apiClient.post('/v1/invoice-documents', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  });
  return InvoiceDocumentSchema.parse(data);
}

export async function listDocuments(page = 0, size = 50): Promise<DocumentPage> {
  const { data } = await apiClient.get('/v1/invoice-documents', { params: { page, size } });
  return DocumentPageSchema.parse(data);
}

export async function getDocument(documentId: string): Promise<InvoiceDocument> {
  const { data } = await apiClient.get(`/v1/invoice-documents/${documentId}`);
  return InvoiceDocumentSchema.parse(data);
}

export async function deleteDocument(documentId: string): Promise<void> {
  await apiClient.delete(`/v1/invoice-documents/${documentId}`);
}

export async function startExtraction(documentId: string): Promise<InvoiceDocumentJob> {
  const { data } = await apiClient.post(`/v1/invoice-documents/${documentId}/extract`);
  return InvoiceDocumentJobSchema.parse(data);
}

export async function getJob(jobId: string): Promise<InvoiceDocumentJob> {
  const { data } = await apiClient.get(`/v1/invoice-jobs/${jobId}`);
  return InvoiceDocumentJobSchema.parse(data);
}

export async function getExtraction(documentId: string): Promise<InvoiceExtraction> {
  const { data } = await apiClient.get(`/v1/invoice-documents/${documentId}/extraction`);
  return InvoiceExtractionSchema.parse(data);
}

export async function confirmField(
  documentId: string,
  fieldId: string,
  fieldValue: string
): Promise<InvoiceField> {
  const { data } = await apiClient.post(
    `/v1/invoice-documents/${documentId}/fields/${fieldId}/confirm`,
    { fieldValue }
  );
  return InvoiceFieldSchema.parse(data);
}

export async function approveExtraction(documentId: string): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(`/v1/invoice-documents/${documentId}/approve`);
  return InvoiceExtractionSchema.parse(data);
}

export async function rejectExtraction(documentId: string): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(`/v1/invoice-documents/${documentId}/reject`);
  return InvoiceExtractionSchema.parse(data);
}

export async function postExtraction(documentId: string): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(`/v1/invoice-documents/${documentId}/post`);
  return InvoiceExtractionSchema.parse(data);
}

export async function exportExtraction(
  documentId: string,
  format: 'csv' | 'json' = 'csv'
): Promise<string> {
  const { data } = await apiClient.get(`/v1/invoice-documents/${documentId}/export`, {
    params: { format },
    responseType: 'text',
  });
  return typeof data === 'string' ? data : JSON.stringify(data);
}

/** Original bytes are only reachable with a bearer token, so fetch as a blob. */
export async function fetchDocumentBlob(documentId: string): Promise<Blob> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  const res = await fetch(`${API_BASE}/v1/invoice-documents/${documentId}/content`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    throw new Error(`Download failed (${res.status})`);
  }
  return res.blob();
}
