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
    HIGH: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
    REVIEW: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
    LOW: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
    UNKNOWN: 'bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
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
      <header className="border-b border-gray-200 bg-white dark:bg-gray-900 dark:border-gray-700">
        <div className="max-w-5xl mx-auto px-4 h-16 flex justify-between items-center">
          <Link href="/" className="text-xl font-bold text-gray-900 dark:text-white">
            Fixna Invoice Assistant
          </Link>
          <button
            onClick={onDownloadOriginal}
            className="px-4 py-2 text-sm font-medium text-white bg-gray-900 dark:bg-white dark:text-gray-900 rounded-lg"
          >
            Download original
          </button>
        </div>
      </header>

      <div className="max-w-5xl mx-auto px-4 py-8 space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-bold text-gray-900 dark:text-white">Review document</h1>
          <div className="flex gap-3">
            {!extraction && (
              <button
                onClick={onExtract}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-white bg-gray-900 dark:bg-white dark:text-gray-900 rounded-lg disabled:opacity-50"
              >
                {busy ? 'Working…' : 'Extract fields'}
              </button>
            )}
            {extraction && (
              <>
                <button
                  onClick={() => onExport('csv')}
                  className="px-4 py-2 text-sm font-medium border border-gray-300 dark:border-gray-600 rounded-lg text-gray-800 dark:text-gray-200"
                >
                  Export CSV
                </button>
                <button
                  onClick={() => onExport('json')}
                  className="px-4 py-2 text-sm font-medium border border-gray-300 dark:border-gray-600 rounded-lg text-gray-800 dark:text-gray-200"
                >
                  Export JSON
                </button>
              </>
            )}
          </div>
        </div>

        {notice && <p className="text-sm text-green-700 dark:text-green-400">{notice}</p>}
        {error && <p className="text-sm text-red-600">{error}</p>}

        {loading ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">Loading…</p>
        ) : !extraction ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No extraction yet. Click “Extract fields” to parse this document.
          </p>
        ) : (
          <div className="space-y-6">
            <section className="bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Status: {extraction.status}
                </h2>
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  native pages: {extraction.methodNativePages} · OCR pages: {extraction.methodOcrPages}
                </span>
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
                Extracted values are untrusted until you review and confirm them. Low-confidence fields must
                be corrected before approval.
              </p>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700">
                    <th className="py-2 pr-4">Field</th>
                    <th className="py-2 pr-4">Value</th>
                    <th className="py-2 pr-4">Confidence</th>
                    <th className="py-2 pr-4">Source</th>
                    <th className="py-2" />
                  </tr>
                </thead>
                <tbody>
                  {extraction.fields.map((field) => (
                    <tr key={field.id} className="border-b border-gray-100 dark:border-gray-800">
                      <td className="py-2 pr-4 font-medium text-gray-900 dark:text-white">{field.fieldKey}</td>
                      <td className="py-2 pr-4 text-gray-800 dark:text-gray-200">
                        {field.moneyAmount != null
                          ? formatMoney(field.moneyAmount, field.currency)
                          : field.fieldValue ?? '—'}
                      </td>
                      <td className="py-2 pr-4">{tierBadge(field)}</td>
                      <td className="py-2 pr-4 text-gray-500 dark:text-gray-400">{field.source}</td>
                      <td className="py-2">
                        <div className="flex gap-2 justify-end">
                          <input
                            value={edits[field.id] ?? field.fieldValue ?? ''}
                            onChange={(e) => setEdits((prev) => ({ ...prev, [field.id]: e.target.value }))}
                            className="px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-800 text-gray-900 dark:text-white"
                          />
                          <button
                            onClick={() => onConfirm(field)}
                            disabled={edits[field.id] == null}
                            className="px-3 py-1 text-xs font-medium text-white bg-gray-900 dark:bg-white dark:text-gray-900 rounded disabled:opacity-40"
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
                className="px-4 py-2 text-sm font-medium text-white bg-green-700 rounded-lg disabled:opacity-50"
              >
                Approve
              </button>
              <button
                onClick={() => runAction(() => rejectExtraction(documentId), 'Rejected.')}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-white bg-red-700 rounded-lg disabled:opacity-50"
              >
                Reject
              </button>
              <button
                onClick={() => runAction(() => postExtraction(documentId), 'Posted to invoice record.')}
                disabled={busy}
                className="px-4 py-2 text-sm font-medium text-white bg-gray-900 dark:bg-white dark:text-gray-900 rounded-lg disabled:opacity-50"
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
