import { z } from "zod";
import { apiClient } from "./api-client";

export const MeetingSchema = z.object({
  id: z.string(),
  clientId: z.string(),
  engagementId: z.string().nullable().optional(),
  projectId: z.string().nullable().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  location: z.string().nullable().optional(),
  startsAt: z.string(),
  endsAt: z.string(),
  organizerUserId: z.string(),
  status: z.string(),
  calendarProvider: z.string(),
  calendarEventId: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Meeting = z.infer<typeof MeetingSchema>;

export const MeetingInputSchema = z.object({
  clientId: z.string().min(1),
  engagementId: z.string().optional(),
  projectId: z.string().optional(),
  title: z.string().min(1).max(255),
  description: z.string().optional(),
  location: z.string().max(500).optional(),
  startsAt: z.string().min(1),
  endsAt: z.string().min(1),
});

export type MeetingInput = z.infer<typeof MeetingInputSchema>;

export async function listMeetings(params?: {
  clientId?: string;
  projectId?: string;
}): Promise<Meeting[]> {
  const search = new URLSearchParams();
  if (params?.clientId) {
    search.set("clientId", params.clientId);
  }
  if (params?.projectId) {
    search.set("projectId", params.projectId);
  }
  const query = search.toString();
  const path = query ? `/meetings?${query}` : "/meetings";
  const { data } = await apiClient.get(path);
  return z.array(MeetingSchema).parse(data);
}

export async function createMeeting(input: MeetingInput): Promise<Meeting> {
  const { data } = await apiClient.post("/meetings", input);
  return MeetingSchema.parse(data);
}

export async function transitionMeeting(id: string, status: string): Promise<Meeting> {
  const { data } = await apiClient.post(`/meetings/${id}/transition`, { status });
  return MeetingSchema.parse(data);
}
