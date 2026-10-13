"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import {
  listWebsiteAudits,
  submitWebsiteAuditJob,
  getWebsiteAuditJob,
  cancelWebsiteAuditJob,
  WebsiteAuditInputSchema,
  type WebsiteAuditInput,
  type WebsiteAuditJob,
} from "@/lib/website-audit-api";
import { ErrorState, LoadingState } from "../providers";

const TERMINAL_JOB_STATUSES = new Set(["COMPLETED", "FAILED", "CANCELLED"]);

export default function WebsiteAuditsPage() {
  const [error, setError] = useState<unknown>(null);
  const [activeJob, setActiveJob] = useState<WebsiteAuditJob | null>(null);
  const pollTimer = useRef<number | null>(null);
  const queryClient = useQueryClient();

  const audits = useQuery({
    queryKey: ["website-audits"],
    queryFn: listWebsiteAudits,
    retry: false,
  });

  const runForm = useForm<WebsiteAuditInput>({
    resolver: zodResolver(WebsiteAuditInputSchema),
    defaultValues: { url: "" },
  });

  useEffect(() => () => stopPolling(), []);

  function stopPolling() {
    if (pollTimer.current !== null) {
      window.clearTimeout(pollTimer.current);
      pollTimer.current = null;
    }
  }

  function pollJob(jobId: string, delay: number) {
    stopPolling();
    pollTimer.current = window.setTimeout(async () => {
      try {
        const job = await getWebsiteAuditJob(jobId);
        setActiveJob(job);
        if (TERMINAL_JOB_STATUSES.has(job.status)) {
          stopPolling();
          await queryClient.invalidateQueries({ queryKey: ["website-audits"] });
          return;
        }
        pollJob(jobId, delay);
      } catch (err) {
        stopPolling();
        setError(err);
      }
    }, delay);
  }

  const submitJob = useMutation({
    mutationFn: submitWebsiteAuditJob,
    onSuccess: (job) => {
      setError(null);
      setActiveJob(job);
      pollJob(job.jobId, 2000);
    },
    onError: (err) => setError(err),
  });

  const cancelJob = useMutation({
    mutationFn: cancelWebsiteAuditJob,
    onSuccess: async (job) => {
      setActiveJob(job);
      stopPolling();
      await queryClient.invalidateQueries({ queryKey: ["website-audits"] });
    },
    onError: (err) => setError(err),
  });

  return (
    <section aria-label="Website audits">
      <h1>Website audits</h1>
      <p className="muted">
        Deterministic checks for SEO and technical health — title, meta tags, Open Graph, HTTPS,
        response time, robots.txt, and sitemap. Scoring never uses AI.
      </p>

      <form
        onSubmit={runForm.handleSubmit((values) => {
          setError(null);
          setActiveJob(null);
          stopPolling();
          submitJob.mutate(values);
        })}
      >
        <h2>Run an audit</h2>
        <label>
          Website URL
          <input placeholder="https://example.com" {...runForm.register("url")} />
        </label>
        {runForm.formState.errors.url ? (
          <p role="alert">{runForm.formState.errors.url.message}</p>
        ) : null}
        <button type="submit" disabled={submitJob.isPending}>
          {submitJob.isPending ? "Submitting…" : "Run audit (bounded crawl)"}
        </button>
      </form>

      {activeJob ? (
        <section aria-label="Active audit job" className="audit-job">
          <h2>Audit job</h2>
          <p className="muted">
            Status: <strong>{activeJob.status}</strong> · Attempt {activeJob.attempt}/
            {activeJob.maxAttempts} · Pages {activeJob.progress.pagesFetched}/
            {activeJob.progress.budgetPages}
          </p>
          {activeJob.failureCode ? (
            <p role="alert">Failed: {activeJob.failureCode}</p>
          ) : null}
          <div className="audit-job-actions">
            {!TERMINAL_JOB_STATUSES.has(activeJob.status) ? (
              <button
                type="button"
                disabled={cancelJob.isPending}
                onClick={() => cancelJob.mutate(activeJob.jobId)}
              >
                {cancelJob.isPending ? "Cancelling…" : "Cancel"}
              </button>
            ) : null}
            {activeJob.auditId ? (
              <Link href={`/website-audits/${activeJob.auditId}`}>View results</Link>
            ) : null}
          </div>
        </section>
      ) : null}

      {audits.isLoading ? <LoadingState label="Loading audits" /> : null}

      {audits.data?.length ? (
        <section>
          <h2>Recent audits</h2>
          <ul className="resource-list">
            {audits.data.map((audit) => (
              <li key={audit.id}>
                <strong>{audit.inputUrl}</strong>
                <p className="muted">
                  Status: {audit.status} · Score: {audit.overallScore}/100 ·{" "}
                  {new Date(audit.createdAt).toLocaleString()}
                </p>
                <p className="muted">{audit.summary}</p>
                <Link href={`/website-audits/${audit.id}`}>View checks</Link>
              </li>
            ))}
          </ul>
        </section>
      ) : audits.isSuccess && audits.data?.length === 0 ? (
        <p className="muted">No audits yet. Run your first audit above.</p>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
      {audits.error ? <ErrorState error={audits.error} /> : null}
    </section>
  );
}
