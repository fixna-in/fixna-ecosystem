import { z } from "zod";
import { apiClient } from "./api-client";

export const ClientSchema = z.object({
  id: z.string(),
  name: z.string(),
  email: z.string().nullable().optional(),
  phone: z.string().nullable().optional(),
  websiteUrl: z.string().nullable().optional(),
  industry: z.string().nullable().optional(),
  notes: z.string().nullable().optional(),
  status: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Client = z.infer<typeof ClientSchema>;

export const ClientInputSchema = z.object({
  name: z.string().min(1).max(255),
  email: z.string().email().optional().or(z.literal("")),
  phone: z.string().max(50).optional(),
  websiteUrl: z.string().max(1000).optional(),
  industry: z.string().max(100).optional(),
  notes: z.string().optional(),
});

export type ClientInput = z.infer<typeof ClientInputSchema>;

export async function listClients(): Promise<Client[]> {
  const { data } = await apiClient.get("/clients");
  return z.array(ClientSchema).parse(data);
}

export async function createClient(input: ClientInput): Promise<Client> {
  const payload = {
    ...input,
    email: input.email || undefined,
  };
  const { data } = await apiClient.post("/clients", payload);
  return ClientSchema.parse(data);
}
