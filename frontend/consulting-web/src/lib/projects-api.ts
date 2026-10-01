import { z } from "zod";
import { apiClient } from "./api-client";

export const EngagementSchema = z.object({
  id: z.string(),
  clientId: z.string(),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: z.string(),
  startDate: z.string().nullable().optional(),
  endDate: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Engagement = z.infer<typeof EngagementSchema>;

export const EngagementInputSchema = z.object({
  title: z.string().min(1).max(255),
  description: z.string().optional(),
});

export type EngagementInput = z.infer<typeof EngagementInputSchema>;

export const ProjectSchema = z.object({
  id: z.string(),
  engagementId: z.string(),
  name: z.string(),
  description: z.string().nullable().optional(),
  status: z.string(),
  startDate: z.string().nullable().optional(),
  endDate: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Project = z.infer<typeof ProjectSchema>;

export const ProjectInputSchema = z.object({
  engagementId: z.string().min(1),
  name: z.string().min(1).max(255),
  description: z.string().optional(),
});

export type ProjectInput = z.infer<typeof ProjectInputSchema>;

export const TaskSchema = z.object({
  id: z.string(),
  projectId: z.string(),
  milestoneId: z.string().nullable().optional(),
  title: z.string(),
  description: z.string().nullable().optional(),
  status: z.string(),
  dueDate: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type Task = z.infer<typeof TaskSchema>;

export const TaskInputSchema = z.object({
  title: z.string().min(1).max(255),
  description: z.string().optional(),
});

export type TaskInput = z.infer<typeof TaskInputSchema>;

export async function listEngagements(clientId: string): Promise<Engagement[]> {
  const { data } = await apiClient.get(`/clients/${clientId}/engagements`);
  return z.array(EngagementSchema).parse(data);
}

export async function createEngagement(
  clientId: string,
  input: EngagementInput,
): Promise<Engagement> {
  const { data } = await apiClient.post(`/clients/${clientId}/engagements`, input);
  return EngagementSchema.parse(data);
}

export async function transitionEngagement(id: string, status: string): Promise<Engagement> {
  const { data } = await apiClient.post(`/engagements/${id}/transition`, { status });
  return EngagementSchema.parse(data);
}

export async function listProjects(): Promise<Project[]> {
  const { data } = await apiClient.get("/projects");
  return z.array(ProjectSchema).parse(data);
}

export async function createProject(input: ProjectInput): Promise<Project> {
  const { data } = await apiClient.post("/projects", input);
  return ProjectSchema.parse(data);
}

export async function transitionProject(id: string, status: string): Promise<Project> {
  const { data } = await apiClient.post(`/projects/${id}/transition`, { status });
  return ProjectSchema.parse(data);
}

export async function listTasks(projectId: string): Promise<Task[]> {
  const { data } = await apiClient.get(`/projects/${projectId}/tasks`);
  return z.array(TaskSchema).parse(data);
}

export async function createTask(projectId: string, input: TaskInput): Promise<Task> {
  const { data } = await apiClient.post(`/projects/${projectId}/tasks`, input);
  return TaskSchema.parse(data);
}

export async function transitionTask(id: string, status: string): Promise<Task> {
  const { data } = await apiClient.post(`/tasks/${id}/transition`, { status });
  return TaskSchema.parse(data);
}
