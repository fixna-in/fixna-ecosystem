"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { listClients } from "@/lib/clients-api";
import {
  suggestMeetingAgenda,
  suggestProposal,
  summarizeClient,
  suggestWebsiteCopy,
} from "@/lib/ai-api";
import { ErrorState, LoadingState } from "../providers";

type Tab = "proposal" | "meeting" | "client" | "website";

export default function AiAssistantPage() {
  const [tab, setTab] = useState<Tab>("proposal");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [disclaimer, setDisclaimer] = useState<string | null>(null);
  const [result, setResult] = useState<unknown>(null);

  const [proposalClientId, setProposalClientId] = useState("");
  const [proposalTopic, setProposalTopic] = useState("");
  const [meetingTitle, setMeetingTitle] = useState("");
  const [meetingClientId, setMeetingClientId] = useState("");
  const [summaryClientId, setSummaryClientId] = useState("");

  const clients = useQuery({ queryKey: ["clients"], queryFn: listClients, retry: false });

  async function run<T>(action: () => Promise<{ disclaimer: string; suggestion: T }>) {
    setError(null);
    setBusy(true);
    setResult(null);
    setDisclaimer(null);
    try {
      const response = await action();
      setDisclaimer(response.disclaimer);
      setResult(response.suggestion);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section aria-label="AI Assistant">
      <h1>AI Assistant</h1>
      <p className="muted">
        Advisory suggestions for proposals, meetings, clients, and website copy. Review before use.
      </p>

      <div role="tablist" aria-label="AI features" className="tab-row">
        {(
          [
            ["proposal", "Proposal"],
            ["meeting", "Meeting"],
            ["client", "Client"],
            ["website", "Website"],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={tab === id}
            className={tab === id ? "active" : undefined}
            onClick={() => {
              setTab(id);
              setResult(null);
              setDisclaimer(null);
              setError(null);
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {clients.isLoading ? <LoadingState label="Loading clients" /> : null}
      {clients.error ? <ErrorState error={clients.error} /> : null}

      {tab === "proposal" ? (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            run(() => suggestProposal(proposalClientId, proposalTopic || undefined));
          }}
        >
          <h2>Proposal draft</h2>
          <label>
            Client
            <select
              required
              value={proposalClientId}
              onChange={(event) => setProposalClientId(event.target.value)}
            >
              <option value="">Select a client</option>
              {clients.data?.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Topic (optional)
            <input value={proposalTopic} onChange={(event) => setProposalTopic(event.target.value)} />
          </label>
          <button type="submit" disabled={busy || !proposalClientId}>
            {busy ? "Generating…" : "Suggest proposal"}
          </button>
        </form>
      ) : null}

      {tab === "meeting" ? (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            run(() =>
              suggestMeetingAgenda(meetingTitle, meetingClientId || undefined),
            );
          }}
        >
          <h2>Meeting agenda</h2>
          <label>
            Meeting title
            <input
              required
              value={meetingTitle}
              onChange={(event) => setMeetingTitle(event.target.value)}
            />
          </label>
          <label>
            Client (optional)
            <select value={meetingClientId} onChange={(event) => setMeetingClientId(event.target.value)}>
              <option value="">None</option>
              {clients.data?.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={busy || !meetingTitle}>
            {busy ? "Generating…" : "Suggest agenda"}
          </button>
        </form>
      ) : null}

      {tab === "client" ? (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            run(() => summarizeClient(summaryClientId));
          }}
        >
          <h2>Client summary</h2>
          <label>
            Client
            <select
              required
              value={summaryClientId}
              onChange={(event) => setSummaryClientId(event.target.value)}
            >
              <option value="">Select a client</option>
              {clients.data?.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={busy || !summaryClientId}>
            {busy ? "Generating…" : "Summarize client"}
          </button>
        </form>
      ) : null}

      {tab === "website" ? (
        <form
          onSubmit={(event) => {
            event.preventDefault();
            run(() => suggestWebsiteCopy());
          }}
        >
          <h2>Website copy</h2>
          <p className="muted">Suggestions are based on your current consulting profile.</p>
          <button type="submit" disabled={busy}>
            {busy ? "Generating…" : "Suggest copy"}
          </button>
        </form>
      ) : null}

      {error ? <ErrorState error={error} /> : null}

      {disclaimer ? <p className="muted" role="note">{disclaimer}</p> : null}

      {result ? (
        <pre className="ai-result">{JSON.stringify(result, null, 2)}</pre>
      ) : null}
    </section>
  );
}
