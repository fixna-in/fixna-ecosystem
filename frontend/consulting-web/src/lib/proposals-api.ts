import { z } from "zod";
import { apiClient } from "./api-client";

export const ProposalItemSchema = z.object({
  id: z.string(),
  description: z.string(),
  quantity: z.union([z.string(), z.number()]).transform(String),
  unitPrice: z.union([z.string(), z.number()]).transform(String),
  lineTotal: z.union([z.string(), z.number()]).transform(String),
  sortOrder: z.number(),
});

export type ProposalItem = z.infer<typeof ProposalItemSchema>;

export const ProposalSchema = z.object({
  id: z.string(),
  clientId: z.string(),
  engagementId: z.string().nullable().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: z.string(),
  subtotal: z.union([z.string(), z.number()]).transform(String),
  taxAmount: z.union([z.string(), z.number()]).transform(String),
  totalAmount: z.union([z.string(), z.number()]).transform(String),
  currency: z.string(),
  validUntil: z.string().nullable().optional(),
  sentAt: z.string().nullable().optional(),
  items: z.array(ProposalItemSchema).optional().default([]),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Proposal = z.infer<typeof ProposalSchema>;

export const ProposalInputSchema = z.object({
  clientId: z.string().min(1),
  engagementId: z.string().optional(),
  title: z.string().min(1).max(255),
  description: z.string().optional(),
  currency: z.string().length(3).optional(),
  validUntil: z.string().optional(),
});

export type ProposalInput = z.infer<typeof ProposalInputSchema>;

export const ProposalItemInputSchema = z.object({
  description: z.string().min(1),
  quantity: z.coerce.number().positive(),
  unitPrice: z.coerce.number(),
  sortOrder: z.coerce.number().int().optional(),
});

export type ProposalItemInput = z.infer<typeof ProposalItemInputSchema>;

export async function listProposals(clientId: string): Promise<Proposal[]> {
  const { data } = await apiClient.get("/proposals", { params: { clientId } });
  return z.array(ProposalSchema).parse(data);
}

export async function getProposal(id: string): Promise<Proposal> {
  const { data } = await apiClient.get(`/proposals/${id}`);
  return ProposalSchema.parse(data);
}

export async function createProposal(input: ProposalInput): Promise<Proposal> {
  const { data } = await apiClient.post("/proposals", input);
  return ProposalSchema.parse(data);
}

export async function updateProposal(id: string, input: ProposalInput): Promise<Proposal> {
  const { data } = await apiClient.put(`/proposals/${id}`, input);
  return ProposalSchema.parse(data);
}

export async function addProposalItem(id: string, input: ProposalItemInput): Promise<ProposalItem> {
  const { data } = await apiClient.post(`/proposals/${id}/items`, input);
  return ProposalItemSchema.parse(data);
}

export async function updateProposalItem(
  proposalId: string,
  itemId: string,
  input: ProposalItemInput,
): Promise<ProposalItem> {
  const { data } = await apiClient.put(`/proposals/${proposalId}/items/${itemId}`, input);
  return ProposalItemSchema.parse(data);
}

export async function deleteProposalItem(proposalId: string, itemId: string): Promise<Proposal> {
  const { data } = await apiClient.delete(`/proposals/${proposalId}/items/${itemId}`);
  return ProposalSchema.parse(data);
}

export async function sendProposal(id: string): Promise<Proposal> {
  const { data } = await apiClient.post(`/proposals/${id}/send`);
  return ProposalSchema.parse(data);
}

export async function approveProposal(id: string): Promise<Proposal> {
  const { data } = await apiClient.post(`/proposals/${id}/approve`);
  return ProposalSchema.parse(data);
}

export async function rejectProposal(id: string): Promise<Proposal> {
  const { data } = await apiClient.post(`/proposals/${id}/reject`);
  return ProposalSchema.parse(data);
}

export async function cancelProposal(id: string): Promise<Proposal> {
  const { data } = await apiClient.post(`/proposals/${id}/cancel`);
  return ProposalSchema.parse(data);
}
