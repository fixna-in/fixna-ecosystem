import { z } from "zod";
import { apiClient } from "./api-client";

const AiSuggestionEnvelopeSchema = <T extends z.ZodTypeAny>(suggestionSchema: T) =>
  z.object({
    disclaimer: z.string(),
    suggestion: suggestionSchema,
  });

const ProposalLineItemSchema = z.object({
  description: z.string(),
  quantity: z.union([z.string(), z.number()]).transform(String),
  unitPrice: z.union([z.string(), z.number()]).transform(String),
});

export const ProposalSuggestionSchema = z.object({
  title: z.string(),
  lineItems: z.array(ProposalLineItemSchema),
});

export type ProposalSuggestion = z.infer<typeof ProposalSuggestionSchema>;

export const MeetingAgendaSuggestionSchema = z.object({
  bullets: z.array(z.string()),
});

export type MeetingAgendaSuggestion = z.infer<typeof MeetingAgendaSuggestionSchema>;

export const ClientSummarySuggestionSchema = z.object({
  summary: z.string(),
  nextSteps: z.array(z.string()),
});

export type ClientSummarySuggestion = z.infer<typeof ClientSummarySuggestionSchema>;

export const WebsiteCopySuggestionSchema = z.object({
  tagline: z.string(),
  bio: z.string(),
});

export type WebsiteCopySuggestion = z.infer<typeof WebsiteCopySuggestionSchema>;

export async function suggestProposal(clientId: string, topic?: string) {
  const { data } = await apiClient.post("/ai/proposals/suggest", { clientId, topic });
  return AiSuggestionEnvelopeSchema(ProposalSuggestionSchema).parse(data);
}

export async function suggestMeetingAgenda(title: string, clientId?: string) {
  const { data } = await apiClient.post("/ai/meetings/suggest-agenda", { title, clientId });
  return AiSuggestionEnvelopeSchema(MeetingAgendaSuggestionSchema).parse(data);
}

export async function summarizeClient(clientId: string) {
  const { data } = await apiClient.post(`/ai/clients/${clientId}/summarize`);
  return AiSuggestionEnvelopeSchema(ClientSummarySuggestionSchema).parse(data);
}

export async function suggestWebsiteCopy() {
  const { data } = await apiClient.post("/ai/website/suggest-copy");
  return AiSuggestionEnvelopeSchema(WebsiteCopySuggestionSchema).parse(data);
}
