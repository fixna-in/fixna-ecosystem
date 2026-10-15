'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import {
  InvoiceExtraction,
  InvoiceField,
  approveExtraction,
  confirmField,
  exportExtraction,
  fetchDocumentBlob,
  getExtraction,
  postExtraction,
  rejectExtraction,
  startExtraction,
} from '@/lib/invoice-api';
import { confidenceTier, formatMoney } from '@/lib/display';

function tierBadge(field: InvoiceField) {
  const { tier, label } = confidenceTier(field.confidence);
  const styles: Record<string, string> = {
    HIGH: 'bg-success-soft text-success',
    REVIEW: 'bg-warning-soft text-warning',
    LOW: 'bg-surface-muted text-muted-foreground',
    UNKNOWN: 'bg-surface-muted text-muted-foreground',
  };
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${styles[tier]}`}>{label}</span>
  );
}

export default function DocumentDetail() {
  const router = useRouter();
  const params = useParams<{ documentId: string }>();
  const documentId = params.documentId;
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const [extraction, setExtraction] = useState<InvoiceExtraction | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [edits, setEdits] = useState<Record<string, string>>({});

  const load = useCallback(async () => {
    try {
      setExtraction(await getExtraction(documentId));
    } catch (err: unknown) {
      const res = (err as { response?: { status?: number; data?: { message?: string } } }).response;
      if (res?.status !== 404) {
        setError(res?.data?.message || 'Failed to load extraction');
      }
    } finally {
      setLoading(false);
    }
  }, [documentId]);

  useEffect(() => {
    if (!localStorage.getItem('accessToken')) {
      router.replace('/login');
      return;
    }
    load();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [load, router]);


  const onExtract = async () => {
    setError(null);
    setNotice(null);
    setBusy(true);
    try {
      await startExtraction(documentId);
      setNotice('Extraction started…');
      if (pollRef.current) clearInterval(pollRef.current);
      pollRef.current = setInterval(async () => {
        try {
          await load();
          setBusy(false);
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
        } catch {
          /* keep polling until server resolves */
        }
      }, 2000);
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string; code?: string } } }).response;
      setError(res?.data?.message || res?.data?.code || 'Extraction failed');
      setBusy(false);
    }
  };

  const onConfirm = async (field: InvoiceField) => {
    const value = edits[field.id];
    if (value == null) return;
    setError(null);
    try {
      await confirmField(documentId, field.id, value);
      setEdits((prev) => {
        const next = { ...prev };
        delete next[field.id];
        return next;
      });
      await load();
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string } } }).response;
      setError(res?.data?.message || 'Failed to save field');
    }
  };

  const runAction = async (action: () => Promise<InvoiceExtraction>, successMsg: string) => {
    setError(null);
    setBusy(true);
    try {
      setExtraction(await action());
      setNotice(successMsg);
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string; code?: string } } }).response;
      setError(res?.data?.message || res?.data?.code || 'Action failed');
    } finally {
      setBusy(false);
    }
  };

  const onExport = async (format: 'csv' | 'json') => {
    setError(null);
    try {
      const text = await exportExtraction(documentId, format);
      const blob = new Blob([text], {
        type: format === 'csv' ? 'text/csv' : 'application/json',
      });
      const url = URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = `extraction.${format}`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string } } }).response;
      setError(res?.data?.message || 'Export failed');
    }
  };

  const onDownloadOriginal = async () => {
    setError(null);
    try {
      const blob = await fetchDocumentBlob(documentId);
      const url = URL.createObjectURL(blob);
      const a = window.document.createElement('a');
      a.href = url;
      a.download = 'document.pdf';
      a.click();
      URL.revokeObjectURL(url);
    } catch (err: unknown) {
      setError((err as Error).message || 'Download failed');
    }
  };

  return (
    <main className="min-h-screen">
      <header className="border-b border-border bg-surface">
        <div className="max-w-5xl mx-auto px-4 h-16 flex justify-between items-center">
          <Link href="/" className="text-xl font-bold text-foreground">
            Fixna Invoice Assistant
          </Link>
          <button
            onClick={onDownloadOriginal}
            className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg"
          >
            Download original
          </button>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-foreground">Review document</h1>
          <div className="flex gap-3">
            {!extraction && (
              <button
                onClick={onExtract}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg disabled:opacity-50"
              >
                {busy ? 'Working…' : 'Extract fields'}
              </button>
            )}
            {extraction && (
              <>
                <button
                  onClick={() => onExport('csv')}
                  className="px-4 py-2 text-sm font-medium border border-border rounded-lg text-foreground"
                >
                  Export CSV
                </button>
                <button
                  onClick={() => onExport('json')}
                  className="px-4 py-2 text-sm font-medium border border-border rounded-lg text-foreground"
                >
                  Export JSON
                </button>
              </>
            )}
          </div>
        </div>

        {notice && <p className="text-sm text-success">{notice}</p>}
        {error && <p className="text-sm text-danger">{error}</p>}

        {loading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : !extraction ? (
          <p className="text-sm text-muted-foreground">
            No extraction yet. Click "Extract fields" to parse this document.
          </p>
        ) : (
          <div className="space-y-6">
            <section className="bg-surface border border-border rounded-xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-foreground">
                  Status: {extraction.status}
                </h2>
                <span className="text-xs text-muted-foreground">
                  native pages: {extraction.methodNativePages} · OCR pages: {extraction.methodOcrPages}
                </span>
              </div>
              <p className="text-sm text-muted-foreground mb-4">
                Extracted values are untrusted until you review and confirm them. Low-confidence fields must
                be corrected before approval.
              </p>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-muted-foreground border-b border-border">
                    <th className="py-2 pr-4">Field</th>
                    <th className="py-2 pr-4">Value</th>
                    <th className="py-2 pr-4">Confidence</th>
                    <th className="py-2 pr-4">Source</th>
                    <th className="py-2" />
                  </tr>
                </thead>
                <tbody>
                  {extraction.fields.map((field) => (
                    <tr key={field.id} className="border-b border-border">
                      <td className="py-2 pr-4 font-medium text-foreground">{field.fieldKey}</td>
                      <td className="py-2 pr-4 text-foreground">
                        {field.moneyAmount != null
                          ? formatMoney(field.moneyAmount, field.currency)
                          : field.fieldValue ?? '—'}
                      </td>
                      <td className="py-2 pr-4">{tierBadge(field)}</td>
                      <td className="py-2 pr-4 text-muted-foreground">{field.source}</td>
                      <td className="py-2">
                        <div className="flex gap-2 justify-end">
                          <input
                            value={edits[field.id] ?? field.fieldValue ?? ''}
                            onChange={(e) => setEdits((prev) => ({ ...prev, [field.id]: e.target.value }))}
                            className="px-2 py-1 border border-border rounded bg-input text-foreground"
                          />
                          <button
                            onClick={() => onConfirm(field)}
                            disabled={edits[field.id] == null}
                            className="px-3 py-1 text-xs font-medium text-primary-foreground bg-primary rounded disabled:opacity-40"
                          >
                            Save
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>

            <section className="flex gap-3">
              <button
                onClick={() => runAction(() => approveExtraction(documentId), 'Approved.')}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-primary-foreground bg-success rounded-lg disabled:opacity-50"
              >
                Approve
              </button>
              <button
                onClick={() => runAction(() => rejectExtraction(documentId), 'Rejected.')}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-primary-foreground bg-danger rounded-lg disabled:opacity-50"
              >
                Reject
              </button>
              <button
                onClick={() => runAction(() => postExtraction(documentId), 'Posted to invoice record.')}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg disabled:opacity-50"
              >
                Post
              </button>
            </section>
          </div>
        )}
      </div>
    </main>
  );
}
