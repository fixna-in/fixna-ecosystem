import { z } from "zod";
import { apiClient } from "./api-client";

export const InvoiceDocumentSchema = z.object({
  documentId: z.string(),
  invoiceId: z.string(),
  fileName: z.string(),
  sizeBytes: z.number(),
  status: z.string(),
  createdAt: z.string(),
});

export type InvoiceDocument = z.infer<typeof InvoiceDocumentSchema>;

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
  extractorVersion: z.string(),
  createdAt: z.string(),
  fields: z.array(InvoiceFieldSchema).optional().default([]),
});

export type InvoiceExtraction = z.infer<typeof InvoiceExtractionSchema>;

export const InvoiceDocumentJobSchema = z.object({
  jobId: z.string(),
  status: z.string(),
  createdAt: z.string(),
});

export type InvoiceDocumentJob = z.infer<typeof InvoiceDocumentJobSchema>;

export const InvoiceExtractionSearchResultSchema = z.object({
  content: z
    .array(
      z.object({
        id: z.string(),
        documentId: z.string(),
        invoiceId: z.string().nullable().optional(),
        status: z.string(),
        createdAt: z.string(),
        fieldCount: z.number().nullable().optional(),
        totalAmount: z.union([z.string(), z.number()]).nullable().optional(),
      }),
    )
    .optional()
    .default([]),
  page: z.number().optional().default(0),
  size: z.number().optional().default(20),
  totalElements: z.number().optional().default(0),
  totalPages: z.number().optional().default(0),
});

export type InvoiceExtractionSearchResult = z.infer<typeof InvoiceExtractionSearchResultSchema>;

export async function uploadInvoiceDocument(
  invoiceId: string,
  file: File,
): Promise<InvoiceDocument> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post(`/invoices/${invoiceId}/documents`, form, {
    headers: { "Content-Type": "multipart/form-data" },
    timeout: 30000,
  });
  return InvoiceDocumentSchema.parse(data);
}

export async function listInvoiceDocuments(invoiceId: string): Promise<InvoiceDocument[]> {
  const { data } = await apiClient.get(`/invoices/${invoiceId}/documents`);
  return z.array(InvoiceDocumentSchema).parse(data);
}

export async function deleteInvoiceDocument(
  invoiceId: string,
  documentId: string,
): Promise<void> {
  await apiClient.delete(`/invoices/${invoiceId}/documents/${documentId}`);
}

export async function extractInvoiceDocument(
  invoiceId: string,
  documentId: string,
): Promise<InvoiceDocumentJob> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/extract`,
  );
  return InvoiceDocumentJobSchema.parse(data);
}

export async function getInvoiceExtractionJob(jobId: string): Promise<InvoiceDocumentJob> {
  const { data } = await apiClient.get(`/invoice-jobs/${jobId}`);
  return InvoiceDocumentJobSchema.parse(data);
}

export async function searchInvoiceExtractions(params?: {
  q?: string;
  status?: string;
  currency?: string;
  page?: number;
  size?: number;
}): Promise<InvoiceExtractionSearchResult> {
  const { data } = await apiClient.get(`/invoice-extractions/search`, { params });
  return InvoiceExtractionSearchResultSchema.parse(data);
}

export async function requestInvoiceDownloadUrl(
  invoiceId: string,
  documentId: string,
  ttlMinutes = 15,
): Promise<{ url: string; expiresAt: string }> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/download-url`,
    null,
    { params: { ttlMinutes } },
  );
  return z.object({ url: z.string(), expiresAt: z.string() }).parse(data);
}

export async function exportInvoiceExtraction(
  invoiceId: string,
  documentId: string,
  format: "csv" | "json" = "csv",
): Promise<string> {
  const { data } = await apiClient.get(
    `/invoices/${invoiceId}/documents/${documentId}/export`,
    { params: { format }, responseType: "text" },
  );
  return typeof data === "string" ? data : JSON.stringify(data);
}

export async function getInvoiceExtraction(
  invoiceId: string,
  documentId: string,
): Promise<InvoiceExtraction> {
  const { data } = await apiClient.get(
    `/invoices/${invoiceId}/documents/${documentId}/extraction`,
  );
  return InvoiceExtractionSchema.parse(data);
}

export async function confirmInvoiceField(
  invoiceId: string,
  documentId: string,
  fieldId: string,
  fieldValue: string,
): Promise<InvoiceField> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/fields/${fieldId}/confirm`,
    { fieldValue },
  );
  return InvoiceFieldSchema.parse(data);
}

export async function approveInvoiceExtraction(
  invoiceId: string,
  documentId: string,
): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/approve`,
  );
  return InvoiceExtractionSchema.parse(data);
}

export async function rejectInvoiceExtraction(
  invoiceId: string,
  documentId: string,
): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/reject`,
  );
  return InvoiceExtractionSchema.parse(data);
}

export async function postInvoiceExtraction(
  invoiceId: string,
  documentId: string,
): Promise<InvoiceExtraction> {
  const { data } = await apiClient.post(
    `/invoices/${invoiceId}/documents/${documentId}/post`,
  );
  return InvoiceExtractionSchema.parse(data);
}

export function invoiceDocumentContentUrl(
  invoiceId: string,
  documentId: string,
): string {
  const base = apiClient.defaults.baseURL ?? "";
  return `${base}/invoices/${invoiceId}/documents/${documentId}/content`;
}