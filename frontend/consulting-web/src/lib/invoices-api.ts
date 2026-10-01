import { z } from "zod";
import { apiClient } from "./api-client";

export const InvoiceLineSchema = z.object({
  id: z.string(),
  description: z.string(),
  quantity: z.union([z.string(), z.number()]).transform(String),
  unitPrice: z.union([z.string(), z.number()]).transform(String),
  lineTotal: z.union([z.string(), z.number()]).transform(String),
  sortOrder: z.number(),
});

export type InvoiceLine = z.infer<typeof InvoiceLineSchema>;

export const InvoiceSchema = z.object({
  id: z.string(),
  clientId: z.string(),
  engagementId: z.string().nullable().optional(),
  proposalId: z.string().nullable().optional(),
  invoiceNumber: z.string(),
  title: z.string(),
  status: z.string(),
  subtotal: z.union([z.string(), z.number()]).transform(String),
  taxAmount: z.union([z.string(), z.number()]).transform(String),
  totalAmount: z.union([z.string(), z.number()]).transform(String),
  amountPaid: z.union([z.string(), z.number()]).transform(String),
  currency: z.string(),
  dueDate: z.string().nullable().optional(),
  sentAt: z.string().nullable().optional(),
  lines: z.array(InvoiceLineSchema).optional().default([]),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Invoice = z.infer<typeof InvoiceSchema>;

export const InvoiceInputSchema = z.object({
  clientId: z.string().min(1),
  engagementId: z.string().optional(),
  proposalId: z.string().optional(),
  title: z.string().min(1).max(255),
  currency: z.string().length(3).optional(),
  dueDate: z.string().optional(),
});

export type InvoiceInput = z.infer<typeof InvoiceInputSchema>;

export const InvoiceLineInputSchema = z.object({
  description: z.string().min(1),
  quantity: z.coerce.number().positive(),
  unitPrice: z.coerce.number(),
  sortOrder: z.coerce.number().int().optional(),
});

export type InvoiceLineInput = z.infer<typeof InvoiceLineInputSchema>;

export const PaymentSchema = z.object({
  id: z.string(),
  invoiceId: z.string(),
  amount: z.union([z.string(), z.number()]).transform(String),
  currency: z.string(),
  paymentMethod: z.string(),
  reference: z.string().nullable().optional(),
  paidAt: z.string(),
  recordedByUserId: z.string(),
  createdAt: z.string(),
});

export type Payment = z.infer<typeof PaymentSchema>;

export const PaymentInputSchema = z.object({
  amount: z.coerce.number().positive(),
  paymentMethod: z.string().min(1).max(50),
  reference: z.string().max(255).optional(),
});

export type PaymentInput = z.infer<typeof PaymentInputSchema>;

export async function listInvoices(clientId: string): Promise<Invoice[]> {
  const { data } = await apiClient.get("/invoices", { params: { clientId } });
  return z.array(InvoiceSchema).parse(data);
}

export async function getInvoice(id: string): Promise<Invoice> {
  const { data } = await apiClient.get(`/invoices/${id}`);
  return InvoiceSchema.parse(data);
}

export async function createInvoice(input: InvoiceInput): Promise<Invoice> {
  const { data } = await apiClient.post("/invoices", input);
  return InvoiceSchema.parse(data);
}

export async function updateInvoice(id: string, input: InvoiceInput): Promise<Invoice> {
  const { data } = await apiClient.put(`/invoices/${id}`, input);
  return InvoiceSchema.parse(data);
}

export async function addInvoiceLine(id: string, input: InvoiceLineInput): Promise<InvoiceLine> {
  const { data } = await apiClient.post(`/invoices/${id}/lines`, input);
  return InvoiceLineSchema.parse(data);
}

export async function deleteInvoiceLine(invoiceId: string, lineId: string): Promise<Invoice> {
  const { data } = await apiClient.delete(`/invoices/${invoiceId}/lines/${lineId}`);
  return InvoiceSchema.parse(data);
}

export async function sendInvoice(id: string): Promise<Invoice> {
  const { data } = await apiClient.post(`/invoices/${id}/send`);
  return InvoiceSchema.parse(data);
}

export async function cancelInvoice(id: string): Promise<Invoice> {
  const { data } = await apiClient.post(`/invoices/${id}/cancel`);
  return InvoiceSchema.parse(data);
}

export async function listPayments(invoiceId: string): Promise<Payment[]> {
  const { data } = await apiClient.get(`/invoices/${invoiceId}/payments`);
  return z.array(PaymentSchema).parse(data);
}

export async function recordPayment(invoiceId: string, input: PaymentInput): Promise<Payment> {
  const { data } = await apiClient.post(`/invoices/${invoiceId}/payments`, input);
  return PaymentSchema.parse(data);
}
