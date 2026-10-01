"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  approvePortalProposal,
  clearPortalSession,
  fetchPortalMe,
  getPortalProposal,
  listPortalInvoices,
  listPortalMeetings,
  listPortalProjects,
  listPortalProposals,
  portalLogin,
  PortalLoginSchema,
  rejectPortalProposal,
  restorePortalSession,
  type PortalLoginInput,
} from "@/lib/portal-api";
import { ErrorState, LoadingState } from "../providers";

type Tab = "projects" | "proposals" | "invoices" | "meetings";

export default function PortalPage() {
  const [sessionReady, setSessionReady] = useState(false);
  const [hasSession, setHasSession] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [tab, setTab] = useState<Tab>("projects");
  const [selectedProposalId, setSelectedProposalId] = useState("");

  const loginForm = useForm<PortalLoginInput>({
    resolver: zodResolver(PortalLoginSchema),
    defaultValues: { email: "", password: "", clientId: "" },
  });

  useEffect(() => {
    let cancelled = false;
    restorePortalSession()
      .then((session) => {
        if (!cancelled) setHasSession(Boolean(session));
      })
      .finally(() => {
        if (!cancelled) setSessionReady(true);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const me = useQuery({
    queryKey: ["portal-me"],
    queryFn: fetchPortalMe,
    enabled: hasSession,
    retry: false,
  });

  const projects = useQuery({
    queryKey: ["portal-projects"],
    queryFn: listPortalProjects,
    enabled: hasSession && tab === "projects",
    retry: false,
  });

  const proposals = useQuery({
    queryKey: ["portal-proposals"],
    queryFn: listPortalProposals,
    enabled: hasSession && tab === "proposals",
    retry: false,
  });

  const proposalDetail = useQuery({
    queryKey: ["portal-proposal", selectedProposalId],
    queryFn: () => getPortalProposal(selectedProposalId),
    enabled: hasSession && tab === "proposals" && Boolean(selectedProposalId),
    retry: false,
  });

  const invoices = useQuery({
    queryKey: ["portal-invoices"],
    queryFn: listPortalInvoices,
    enabled: hasSession && tab === "invoices",
    retry: false,
  });

  const meetings = useQuery({
    queryKey: ["portal-meetings"],
    queryFn: listPortalMeetings,
    enabled: hasSession && tab === "meetings",
    retry: false,
  });

  const isAdmin = me.data?.role === "CLIENT_ADMIN";

  if (!sessionReady) {
    return <LoadingState label="Loading portal" />;
  }

  if (!hasSession) {
    return (
      <section>
        <h1>Client Portal</h1>
        <p className="muted">Sign in with your client account to view projects, proposals, and invoices.</p>
        <form
          onSubmit={loginForm.handleSubmit(async (values) => {
            setError(null);
            try {
              await portalLogin(values);
              setHasSession(true);
            } catch (err) {
              setError(err);
            }
          })}
        >
          <label>
            Email
            <input type="email" autoComplete="email" {...loginForm.register("email")} />
          </label>
          {loginForm.formState.errors.email ? (
            <p role="alert">{loginForm.formState.errors.email.message}</p>
          ) : null}
          <label>
            Password
            <input type="password" autoComplete="current-password" {...loginForm.register("password")} />
          </label>
          {loginForm.formState.errors.password ? (
            <p role="alert">{loginForm.formState.errors.password.message}</p>
          ) : null}
          <label>
            Client ID
            <input placeholder="UUID from your consultant" {...loginForm.register("clientId")} />
          </label>
          {loginForm.formState.errors.clientId ? (
            <p role="alert">{loginForm.formState.errors.clientId.message}</p>
          ) : null}
          <button type="submit" disabled={loginForm.formState.isSubmitting}>
            {loginForm.formState.isSubmitting ? "Signing in…" : "Sign in to portal"}
          </button>
        </form>
        {error ? <ErrorState error={error} /> : null}
        <p>
          Consultant? <Link href="/login">Sign in to workspace</Link>
        </p>
      </section>
    );
  }

  return (
    <section aria-label="Client portal">
      <header className="portal-header">
        <div>
          <h1>{me.data?.clientName ?? "Client Portal"}</h1>
          <p className="muted">
            {me.data ? `Signed in as ${me.data.role.replace("_", " ").toLowerCase()}` : "Loading…"}
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            clearPortalSession();
            setHasSession(false);
            setSelectedProposalId("");
          }}
        >
          Sign out
        </button>
      </header>

      <nav className="portal-tabs" aria-label="Portal views">
        {(["projects", "proposals", "invoices", "meetings"] as Tab[]).map((key) => (
          <button
            key={key}
            type="button"
            className={tab === key ? "active" : undefined}
            onClick={() => setTab(key)}
          >
            {key.charAt(0).toUpperCase() + key.slice(1)}
          </button>
        ))}
      </nav>

      {tab === "projects" ? (
        <div>
          <h2>Projects</h2>
          {projects.isLoading ? <LoadingState label="Loading projects" /> : null}
          {projects.error ? <ErrorState error={projects.error} /> : null}
          {projects.data?.length ? (
            <ul>
              {projects.data.map((project) => (
                <li key={project.id}>
                  <strong>{project.name}</strong> — {project.status}
                </li>
              ))}
            </ul>
          ) : (
            !projects.isLoading && <p className="muted">No projects yet.</p>
          )}
        </div>
      ) : null}

      {tab === "proposals" ? (
        <div>
          <h2>Proposals</h2>
          {proposals.isLoading ? <LoadingState label="Loading proposals" /> : null}
          {proposals.error ? <ErrorState error={proposals.error} /> : null}
          {proposals.data?.length ? (
            <ul>
              {proposals.data.map((proposal) => (
                <li key={proposal.id}>
                  <button type="button" onClick={() => setSelectedProposalId(proposal.id)}>
                    {proposal.title}
                  </button>
                  {" — "}
                  {proposal.status}
                  {proposal.totalAmount ? ` (${proposal.currency} ${proposal.totalAmount})` : null}
                </li>
              ))}
            </ul>
          ) : (
            !proposals.isLoading && <p className="muted">No proposals to review.</p>
          )}
          {selectedProposalId && proposalDetail.data ? (
            <article className="portal-detail">
              <h3>{proposalDetail.data.title}</h3>
              <p>Status: {proposalDetail.data.status}</p>
              {proposalDetail.data.items?.length ? (
                <ul>
                  {proposalDetail.data.items.map((item) => (
                    <li key={item.id}>
                      {item.description} — {item.quantity} × {item.unitPrice}
                    </li>
                  ))}
                </ul>
              ) : null}
              {isAdmin && proposalDetail.data.status === "SENT" ? (
                <div className="portal-actions">
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await approvePortalProposal(selectedProposalId);
                        await proposals.refetch();
                        await proposalDetail.refetch();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Approve
                  </button>
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await rejectPortalProposal(selectedProposalId);
                        await proposals.refetch();
                        await proposalDetail.refetch();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Reject
                  </button>
                </div>
              ) : null}
            </article>
          ) : null}
        </div>
      ) : null}

      {tab === "invoices" ? (
        <div>
          <h2>Invoices</h2>
          {invoices.isLoading ? <LoadingState label="Loading invoices" /> : null}
          {invoices.error ? <ErrorState error={invoices.error} /> : null}
          {invoices.data?.length ? (
            <ul>
              {invoices.data.map((invoice) => (
                <li key={invoice.id}>
                  <strong>{invoice.invoiceNumber}</strong> — {invoice.title} — {invoice.status}
                  {invoice.totalAmount ? ` (${invoice.currency} ${invoice.totalAmount})` : null}
                </li>
              ))}
            </ul>
          ) : (
            !invoices.isLoading && <p className="muted">No invoices yet.</p>
          )}
        </div>
      ) : null}

      {tab === "meetings" ? (
        <div>
          <h2>Meetings</h2>
          {meetings.isLoading ? <LoadingState label="Loading meetings" /> : null}
          {meetings.error ? <ErrorState error={meetings.error} /> : null}
          {meetings.data?.length ? (
            <ul>
              {meetings.data.map((meeting) => (
                <li key={meeting.id}>
                  <strong>{meeting.title}</strong> — {new Date(meeting.startsAt).toLocaleString()}
                </li>
              ))}
            </ul>
          ) : (
            !meetings.isLoading && <p className="muted">No meetings scheduled.</p>
          )}
        </div>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
    </section>
  );
}
