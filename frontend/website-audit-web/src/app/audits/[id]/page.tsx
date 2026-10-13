'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { Audit, AuditCheck, fetchReportBlob, getAudit, suggestFixes } from '@/lib/website-audit-api';
import { ThemeToggle } from '@fixna/design-system';

function severityBadge(severity: string) {
  const styles: Record<string, string> = {
    HIGH: 'bg-danger-soft text-danger',
    MEDIUM: 'bg-warning-soft text-warning',
    LOW: 'bg-surface-muted text-muted-foreground',
    INFO: 'bg-info-soft text-info',
  };
  const cls = styles[severity] ?? styles.INFO;
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${cls}`}>{severity}</span>
  );
}

function CheckRow({ check }: { check: AuditCheck }) {
  const passing = check.score >= 100;
  return (
    <li className="border-b border-border py-3">
      <div className="flex items-center justify-between">
        <span className="font-medium text-foreground">{check.checkCode}</span>
        {passing ? (
          <span className="text-xs text-success">Pass</span>
        ) : (
          severityBadge(check.severity)
        )}
      </div>
      {check.fixHint && (
        <p className="text-sm text-muted-foreground mt-1">{check.fixHint}</p>
      )}
    </li>
  );
}

export default function AuditDetail() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = params.id;
  const [audit, setAudit] = useState<Audit | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fixes, setFixes] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setAudit(await getAudit(id));
    } catch (err: unknown) {
      const res = (err as { response?: { status?: number; data?: { message?: string } } }).response;
      if (res?.status !== 404) {
        setError(res?.data?.message || 'Failed to load audit');
      }
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (!localStorage.getItem('accessToken')) {
      router.replace('/login');
      return;
    }
    load();
  }, [load, router]);

  const onDownloadReport = async () => {
    setError(null);
    try {
      const blob = await fetchReportBlob(id);
      const url = URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = `audit-${id}.html`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      setError((err as Error).message || 'Report download failed');
    }
  };

  const onSuggestFixes = async () => {
    setError(null);
    setFixes(null);
    try {
      const res = await suggestFixes(id);
      setFixes(`${res.disclaimer}\n\nPriorities:\n- ${res.suggestion.priorities.join('\n- ')}`);
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string; code?: string } } }).response;
      setError(res?.data?.message || res?.data?.code || 'AI fixes unavailable');
    }
  };

  const failing = audit?.checks.filter((c) => c.score < 100) ?? [];

  return (
    <main className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="max-w-5xl mx-auto px-4 h-16 flex justify-between items-center gap-3">
          <Link href="/" className="text-xl font-bold text-foreground">
            Fixna Website Audit
          </Link>
          <div className="flex items-center gap-3">
            <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            <button
              onClick={onDownloadReport}
              className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover"
            >
              Download report
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
        {loading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : !audit ? (
          <p className="text-sm text-muted-foreground">Audit not found.</p>
        ) : (
          <>
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-xl font-bold text-foreground">{audit.inputUrl}</h1>
                <p className="text-sm text-muted-foreground">
                  {audit.status} · overall score {audit.overallScore} · {new Date(audit.createdAt).toLocaleString()}
                </p>
              </div>
              <button
                onClick={onSuggestFixes}
                className="px-4 py-2 text-sm font-medium border border-border rounded-lg text-foreground hover:bg-surface-muted"
              >
                Suggest fixes (AI)
              </button>
            </div>

            {audit.summary && (
              <p className="text-sm text-foreground bg-surface border border-border rounded-xl p-4">
                {audit.summary}
              </p>
            )}

            {error && <p className="text-sm text-danger">{error}</p>}
            {fixes && (
              <pre className="text-sm whitespace-pre-wrap text-foreground bg-surface-muted rounded-xl p-4">
                {fixes}
              </pre>
            )}

            <section>
              <h2 className="text-lg font-semibold text-foreground mb-2">
                Findings ({failing.length})
              </h2>
              {failing.length === 0 ? (
                <p className="text-sm text-muted-foreground">No failing checks.</p>
              ) : (
                <ul className="bg-surface border border-border rounded-xl px-4">
                  {failing.map((check) => (
                    <CheckRow key={check.id} check={check} />
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
    </main>
  );
}
