'use client';

import { ChangeEvent, useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import {
  InvoiceDocument,
  deleteDocument,
  listDocuments,
  uploadDocument,
} from '@/lib/invoice-api';
import { formatBytes } from '@/lib/display';
import { ThemeToggle } from '@fixna/design-system';

export default function Dashboard() {
  const router = useRouter();
  const [documents, setDocuments] = useState<InvoiceDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef<HTMLInputElement | null>(null);

  const refresh = useCallback(async () => {
    try {
      const page = await listDocuments(0, 50);
      setDocuments(page.content);
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string } } }).response;
      setError(res?.data?.message || 'Failed to load documents');
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
  }, [refresh, router]);

  const onUpload = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setError(null);
    setNotice(null);
    setUploading(true);
    try {
      const doc = await uploadDocument(file);
      setNotice(`Uploaded ${doc.fileName}. Open it to extract fields.`);
      await refresh();
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string; code?: string } } }).response;
      setError(res?.data?.message || res?.data?.code || 'Upload failed');
    } finally {
      setUploading(false);
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const onDelete = async (doc: InvoiceDocument) => {
    if (!window.confirm(`Delete ${doc.fileName}? This removes the file and its extracted data.`)) return;
    setError(null);
    try {
      await deleteDocument(doc.documentId);
      await refresh();
    } catch (err: unknown) {
      const res = (err as { response?: { data?: { message?: string } } }).response;
      setError(res?.data?.message || 'Delete failed');
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
          <span className="text-xl font-bold text-foreground">Fixna Invoice Assistant</span>
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
          <h2 className="text-lg font-semibold text-foreground mb-1">Upload a document</h2>
          <p className="text-sm text-muted-foreground mb-4">
            PDF only, up to 10 MB. Files stay private and are only served back to your workspace.
          </p>
          <input
            ref={fileRef}
            type="file"
            accept="application/pdf"
            onChange={onUpload}
            disabled={uploading}
            className="block w-full text-sm text-foreground file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:font-medium file:bg-primary file:text-primary-foreground disabled:opacity-50"
          />
          {uploading && <p className="mt-3 text-sm text-muted-foreground">Uploading…</p>}
          {notice && <p className="mt-3 text-sm text-success">{notice}</p>}
          {error && <p className="mt-3 text-sm text-danger">{error}</p>}
        </section>

        <section>
          <h2 className="text-lg font-semibold text-foreground mb-4">Documents</h2>
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : documents.length === 0 ? (
            <p className="text-sm text-muted-foreground">No documents yet. Upload a PDF to get started.</p>
          ) : (
            <ul className="divide-y divide-border bg-surface border border-border rounded-xl">
              {documents.map((doc) => (
                <li key={doc.documentId} className="flex items-center justify-between px-4 py-3">
                  <div className="min-w-0">
                    <Link
                      href={`/documents/${doc.documentId}`}
                      className="font-medium text-foreground hover:underline"
                    >
                      {doc.fileName}
                    </Link>
                    <p className="text-xs text-muted-foreground">
                      {formatBytes(doc.sizeBytes)} · {doc.status}
                    </p>
                  </div>
                  <div className="flex items-center gap-3 shrink-0">
                    <Link
                      href={`/documents/${doc.documentId}`}
                      className="text-sm text-link hover:underline"
                    >
                      Review
                    </Link>
                    <button
                      onClick={() => onDelete(doc)}
                      className="text-sm text-danger hover:underline"
                    >
                      Delete
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </main>
  );
}
