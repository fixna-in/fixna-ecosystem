"use client";

import { useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import Link from "next/link";
import { useState } from "react";
import {
  getWebsiteAudit,
  suggestAuditFixes,
  websiteAuditReportUrl,
  type WebsiteAuditFixes,
} from "@/lib/website-audit-api";
import { getAccessToken } from "@/lib/api-client";
import { ErrorState, LoadingState } from "../../providers";

export default function WebsiteAuditDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [error, setError] = useState<unknown>(null);
  const [fixes, setFixes] = useState<WebsiteAuditFixes | null>(null);
  const [fixesBusy, setFixesBusy] = useState(false);

  const audit = useQuery({
    queryKey: ["website-audit", id],
    queryFn: () => getWebsiteAudit(id),
    retry: false,
  });

  async function loadFixes() {
    setError(null);
    setFixesBusy(true);
    try {
      setFixes(await suggestAuditFixes(id));
    } catch (err) {
      setError(err);
    } finally {
      setFixesBusy(false);
    }
  }

  if (audit.isLoading) return <LoadingState label="Loading audit" />;
  if (!audit.data) return <ErrorState error={audit.error ?? new Error("Audit not found")} />;

  const detail = audit.data;
  const failingChecks = detail.checks.filter((check) => check.score < 100);

  async function downloadReport() {
    setError(null);
    try {
      const token = getAccessToken();
      const response = await fetch(websiteAuditReportUrl(detail.id), {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!response.ok) {
        throw new Error("Failed to generate report");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `website-audit-${detail.id}.html`;
      anchor.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setError(err);
    }
  }

  return (
    <section aria-label="Website audit detail">
      <p>
        <Link href="/website-audits">Back to website audits</Link>
      </p>
      <h1>{detail.inputUrl}</h1>
      <p className="muted">
        Status: {detail.status} · Overall score: {detail.overallScore}/100 ·{" "}
        {new Date(detail.createdAt).toLocaleString()}
      </p>
      <p>{detail.summary}</p>

      <p>
        <button type="button" onClick={downloadReport}>
          Download HTML report
        </button>
      </p>

      <section>
        <h2>Checks</h2>
        {detail.checks.length ? (
          <ul className="resource-list">
            {detail.checks.map((check) => (
              <li key={check.id}>
                <strong>
                  {check.checkCode.split("_").join(" ")} — {check.score}/100 ({check.severity})
                </strong>
                {check.fixHint ? <p>{check.fixHint}</p> : null}
                {Object.keys(check.evidence).length ? (
                  <p className="muted">
                    {Object.entries(check.evidence).map(([key, value]) => `${key}: ${value}`).join(" · ")}
                  </p>
                ) : null}
              </li>
            ))}
          </ul>
        ) : (
          <p className="muted">No individual checks were recorded for this audit.</p>
        )}
      </section>

      {failingChecks.length ? (
        <section>
          <h2>Advisory fix suggestions</h2>
          <p className="muted">
            AI-generated guidance for the failing checks above. Advisory only — review before acting.
          </p>
          <button type="button" onClick={loadFixes} disabled={fixesBusy}>
            {fixesBusy ? "Requesting…" : "Suggest fixes with AI"}
          </button>
          {fixes ? (
            <div>
              <p className="muted">{fixes.disclaimer}</p>
              <ul className="resource-list">
                {fixes.suggestion.priorities.map((priority) => (
                  <li key={priority}>{priority}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </section>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
      {audit.error ? <ErrorState error={audit.error} /> : null}
    </section>
  );
}
