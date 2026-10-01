import axios from "axios";
import { z } from "zod";
import { ApiErrorSchema, toApiError } from "./api-client";
import { AuthResponseSchema, type AuthResponse } from "./auth-api";
import { InvoiceSchema } from "./invoices-api";
import { MeetingSchema } from "./meetings-api";
import { ProjectSchema } from "./projects-api";
import { ProposalSchema } from "./proposals-api";

function resolveBaseUrl(): string {
  const raw =
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_URL ??
    "http://localhost:8081/api/v1";
  const trimmed = raw.replace(/\/+$/, "");
  return trimmed.endsWith("/v1") ? trimmed : `${trimmed}/v1`;
}

const PORTAL_REFRESH_KEY = "fixna.portal.refreshToken";

export const portalClient = axios.create({
  baseURL: resolveBaseUrl(),
  withCredentials: false,
  timeout: 15000,
  headers: { "Content-Type": "application/json" },
});

let portalAccessToken: string | null = null;

export function setPortalAccessToken(token: string | null): void {
  portalAccessToken = token;
}

export function getPortalAccessToken(): string | null {
  return portalAccessToken;
}

portalClient.interceptors.request.use((config) => {
  if (portalAccessToken) config.headers.set("Authorization", `Bearer ${portalAccessToken}`);
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    config.headers.set("X-Request-Id", crypto.randomUUID());
  }
  return config;
});

export const PortalLoginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(1),
  clientId: z.string().uuid(),
});

export type PortalLoginInput = z.infer<typeof PortalLoginSchema>;

export const PortalMeSchema = z.object({
  userId: z.string(),
  tenantId: z.string(),
  clientId: z.string(),
  clientName: z.string(),
  role: z.string(),
});

export type PortalMe = z.infer<typeof PortalMeSchema>;

export const PortalInvoiceDetailSchema = z.object({
  invoice: InvoiceSchema,
  payments: z.array(
    z.object({
      id: z.string(),
      invoiceId: z.string(),
      amount: z.union([z.string(), z.number()]).transform(String),
      currency: z.string(),
      paymentMethod: z.string(),
      reference: z.string().nullable().optional(),
      paidAt: z.string(),
      createdAt: z.string(),
    }),
  ),
});

export type PortalInvoiceDetail = z.infer<typeof PortalInvoiceDetailSchema>;

let portalRefreshToken: string | null = null;

export function getPortalRefreshToken(): string | null {
  return portalRefreshToken;
}

async function storePortalAuth(data: unknown): Promise<AuthResponse> {
  const parsed = AuthResponseSchema.parse(data);
  setPortalAccessToken(parsed.accessToken);
  portalRefreshToken = parsed.refreshToken;
  if (typeof window !== "undefined") {
    window.localStorage.setItem(PORTAL_REFRESH_KEY, parsed.refreshToken);
  }
  return parsed;
}

export async function portalLogin(input: PortalLoginInput): Promise<AuthResponse> {
  const { data } = await portalClient.post("/portal/auth/login", input);
  return storePortalAuth(data);
}

export async function refreshPortalSession(): Promise<AuthResponse> {
  const stored =
    portalRefreshToken ??
    (typeof window !== "undefined" ? window.localStorage.getItem(PORTAL_REFRESH_KEY) : null);
  if (!stored) throw new Error("No portal refresh token available");
  const { data } = await portalClient.post("/auth/refresh", { refreshToken: stored });
  return storePortalAuth(data);
}

export function clearPortalSession(): void {
  setPortalAccessToken(null);
  portalRefreshToken = null;
  if (typeof window !== "undefined") {
    window.localStorage.removeItem(PORTAL_REFRESH_KEY);
  }
}

export async function restorePortalSession(): Promise<AuthResponse | null> {
  const stored =
    typeof window !== "undefined" ? window.localStorage.getItem(PORTAL_REFRESH_KEY) : null;
  if (!stored) return null;
  try {
    return await refreshPortalSession();
  } catch {
    clearPortalSession();
    return null;
  }
}

export async function fetchPortalMe(): Promise<PortalMe> {
  const { data } = await portalClient.get("/portal/me");
  return PortalMeSchema.parse(data);
}

export async function listPortalProjects() {
  const { data } = await portalClient.get("/portal/projects");
  return z.array(ProjectSchema).parse(data);
}

export async function listPortalProposals() {
  const { data } = await portalClient.get("/portal/proposals");
  return z.array(ProposalSchema).parse(data);
}

export async function getPortalProposal(id: string) {
  const { data } = await portalClient.get(`/portal/proposals/${id}`);
  return ProposalSchema.parse(data);
}

export async function approvePortalProposal(id: string) {
  const { data } = await portalClient.post(`/portal/proposals/${id}/approve`);
  return ProposalSchema.parse(data);
}

export async function rejectPortalProposal(id: string) {
  const { data } = await portalClient.post(`/portal/proposals/${id}/reject`);
  return ProposalSchema.parse(data);
}

export async function listPortalInvoices() {
  const { data } = await portalClient.get("/portal/invoices");
  return z.array(InvoiceSchema).parse(data);
}

export async function getPortalInvoice(id: string) {
  const { data } = await portalClient.get(`/portal/invoices/${id}`);
  return PortalInvoiceDetailSchema.parse(data);
}

export async function listPortalMeetings() {
  const { data } = await portalClient.get("/portal/meetings");
  return z.array(MeetingSchema).parse(data);
}

export { toApiError, ApiErrorSchema };
