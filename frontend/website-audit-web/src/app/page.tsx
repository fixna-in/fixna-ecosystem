'use client';

import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { Audit, AuditJob, getJob, listAudits, submitJob, terminal } from '@/lib/website-audit-api';
import { ThemeToggle } from '@fixna/design-system';

export default function Dashboard() {
  const router = useRouter();
  const [audits, setAudits] = useState<Audit[]>([]);
  const [url, setUrl] = useState('');
  const [crawl, setCrawl] = useState(true);
  const [activeJob, setActiveJob] = useState<AuditJob | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const refresh = useCallback(async () => {
    try {
      setAudits(await listAudits());
    } catch {
      // token refresh handled by interceptor; hard failures land on /login
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!localStorage.getItem('accessToken')) {
      router.replace('/login');
      return;
    }
    refresh();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [refresh, router]);

  const startPolling = (job: AuditJob) => {
    setActiveJob(job);
    if (pollRef.current) clearInterval(pollRef.current);
    pollRef.current = setInterval(async () => {
      try {
        const next = await getJob(job.jobId);
        setActiveJob(next);
        if (terminal(next)) {
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
          await refresh();
        }
      } catch {
        if (pollRef.current) clearInterval(pollRef.current);
        pollRef.current = null;
      }
    }, 2000);
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      startPolling(await submitJob(url.trim(), crawl));
      setUrl('');
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { code?: string; message?: string } } }).response;
      setError(res?.data?.message || res?.data?.code || 'Failed to submit audit');
    }
  };

  const signOut = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    router.replace('/login');
  };

  return (
    <main className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="max-w-5xl mx-auto px-4 h-16 flex justify-between items-center gap-3">
          <span className="text-xl font-bold text-foreground">Fixna Website Audit</span>
          <div className="flex items-center gap-3">
            <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            <button
              onClick={signOut}
              className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-4 py-8 space-y-8">
        <section className="bg-surface border border-border rounded-xl p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4">Run an audit</h2>
          <form onSubmit={onSubmit} className="flex flex-col sm:flex-row gap-3">
            <input
              type="url"
              required
              placeholder="https://example.com"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              className="flex-1 px-3 py-2 border border-border rounded-lg bg-input text-foreground"
            />
            <label className="flex items-center gap-2 text-sm text-muted-foreground">
              <input type="checkbox" checked={crawl} onChange={(e) => setCrawl(e.target.checked)} />
              Crawl pages
            </label>
            <button
              type="submit"
              disabled={activeJob !== null && !terminal(activeJob)}
              className="px-6 py-2 font-medium text-primary-foreground bg-primary rounded-lg disabled:opacity-50 hover:bg-primary-hover"
            >
              Audit
            </button>
          </form>
          {error && <p className="mt-3 text-sm text-danger">{error}</p>}

          {activeJob && (
            <div className="mt-4 p-4 rounded-lg bg-surface-muted text-sm text-foreground">
              <p>
                Job <span className="font-mono">{activeJob.jobId.slice(0, 8)}</span> —{' '}
                <strong>{activeJob.status}</strong>
                {activeJob.status === 'RUNNING' &&
                  ` · ${activeJob.progress.pagesFetched}/${activeJob.progress.budgetPages} pages`}
                {activeJob.failureCode && ` · ${activeJob.failureCode}`}
              </p>
              {activeJob.status === 'COMPLETED' && activeJob.auditId && (
                <Link href={`/audits/${activeJob.auditId}`} className="text-link hover:underline">
                  View report →
                </Link>
              )}
            </div>
          )}
        </section>
        <section>
          <h2 className="text-lg font-semibold text-foreground mb-4">Recent audits</h2>
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : audits.length === 0 ? (
            <p className="text-sm text-muted-foreground">No audits yet. Run your first audit above.</p>
          ) : (
            <ul className="divide-y divide-border bg-surface border border-border rounded-xl">
              {audits.map((audit) => (
                <li key={audit.id} className="flex items-center justify-between px-4 py-3">
                  <div className="min-w-0">
                    <Link
                      href={`/audits/${audit.id}`}
                      className="font-medium text-foreground hover:underline"
                    >
                      {audit.inputUrl}
                    </Link>
                    <p className="text-xs text-muted-foreground">
                      {audit.status} · score {audit.overallScore} · {new Date(audit.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <Link
                    href={`/audits/${audit.id}`}
                    className="text-sm text-link hover:underline shrink-0"
                  >
                    View
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </main>
  );
}
