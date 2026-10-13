"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  uploadInvoiceDocument,
  listInvoiceDocuments,
  deleteInvoiceDocument,
  extractInvoiceDocument,
  getInvoiceExtractionJob,
  getInvoiceExtraction,
  confirmInvoiceField,
  approveInvoiceExtraction,
  rejectInvoiceExtraction,
  postInvoiceExtraction,
  exportInvoiceExtraction,
  invoiceDocumentContentUrl,
  type InvoiceDocument,
  type InvoiceDocumentJob,
  type InvoiceExtraction,
  type InvoiceField,
} from "@/lib/invoice-documents-api";
import { getAccessToken } from "@/lib/api-client";
import { ErrorState } from "@/app/providers";

const MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

function confidenceClass(confidence: number): string {
  if (confidence >= 0.9) return "confidence-high";
  if (confidence >= 0.7) return "confidence-review";
  return "confidence-low";
}

/** Advisory AI fields never block; sub-0.9 deterministic fields need confirmation. */
function needsConfirmation(field: InvoiceField): boolean {
  const confidence = Number(field.confidence);
  return field.source !== "AI_ADVISORY" && confidence < 0.9 && field.source !== "MANUAL";
}

export function InvoiceDocumentIntake({
  invoiceId,
  onInvoiceChanged,
}: {
  invoiceId: string;
  onInvoiceChanged: () => void;
}) {
  const [documents, setDocuments] = useState<InvoiceDocument[]>([]);
  const [extraction, setExtraction] = useState<InvoiceExtraction | null>(null);
  const [activeDocumentId, setActiveDocumentId] = useState<string>("");
  const [activeJob, setActiveJob] = useState<InvoiceDocumentJob | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [loaded, setLoaded] = useState(false);
  const fileInput = useRef<HTMLInputElement>(null);
  const pollTimer = useRef<number | null>(null);

  const stopJobPolling = useCallback(() => {
    if (pollTimer.current !== null) {
      window.clearTimeout(pollTimer.current);
      pollTimer.current = null;
    }
  }, []);

  useEffect(() => () => stopJobPolling(), [stopJobPolling]);

  const refreshDocuments = useCallback(async () => {
    const docs = await listInvoiceDocuments(invoiceId);
    setDocuments(docs);
    setLoaded(true);
    return docs;
  }, [invoiceId]);

  useEffect(() => {
    setExtraction(null);
    setActiveDocumentId("");
    refreshDocuments().catch((err) => setError(err));
  }, [refreshDocuments]);

  async function handleUpload(file: File) {
    setError(null);
    if (file.type !== "application/pdf") {
      setError(new Error("Only PDF uploads are supported."));
      return;
    }
    if (file.size > MAX_UPLOAD_BYTES) {
      setError(new Error("Uploads are limited to 10 MB."));
      return;
    }
    setBusy(true);
    try {
      await uploadInvoiceDocument(invoiceId, file);
      await refreshDocuments();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const pollExtractionJob = useCallback(
    async (jobId: string, documentId: string, delay = 2000) => {
      stopJobPolling();
      pollTimer.current = window.setTimeout(async () => {
        try {
          const job = await getInvoiceExtractionJob(jobId);
          setActiveJob(job);
          if (job.status === "COMPLETED") {
            stopJobPolling();
            setExtraction(await getInvoiceExtraction(invoiceId, documentId));
            setActiveDocumentId(documentId);
            await refreshDocuments();
            setBusy(false);
            return;
          }
          if (job.status === "FAILED" || job.status === "CANCELLED") {
            stopJobPolling();
            setError(new Error(`Extraction ${job.status.toLowerCase()}. Review the document and retry.`));
            setBusy(false);
            return;
          }
          await pollExtractionJob(jobId, documentId, Math.min(delay + 1000, 5000));
        } catch (err) {
          stopJobPolling();
          setError(err);
          setBusy(false);
        }
      }, delay);
    },
    [invoiceId, refreshDocuments, stopJobPolling],
  );

  async function handleExtract(documentId: string) {
    setError(null);
    setBusy(true);
    stopJobPolling();
    try {
      const job = await extractInvoiceDocument(invoiceId, documentId);
      setActiveJob(job);
      setActiveDocumentId(documentId);
      await pollExtractionJob(job.jobId, documentId);
      await refreshDocuments();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  }

  async function handleViewExtraction(documentId: string) {
    setError(null);
    try {
      setExtraction(await getInvoiceExtraction(invoiceId, documentId));
      setActiveDocumentId(documentId);
    } catch (err) {
      setError(err);
    }
  }

  async function handleConfirm(fieldId: string, fieldValue: string) {
    setError(null);
    try {
      const updated = await confirmInvoiceField(
        invoiceId,
        activeDocumentId,
        fieldId,
        fieldValue,
      );
      setExtraction((prev) =>
        prev ? { ...prev, fields: prev.fields.map((f) => (f.id === fieldId ? updated : f)) } : prev,
      );
    } catch (err) {
      setError(err);
    }
  }

  async function handleAction(action: "approve" | "reject" | "post") {
    setError(null);
    setBusy(true);
    try {
      const call =
        action === "approve"
          ? approveInvoiceExtraction
          : action === "reject"
            ? rejectInvoiceExtraction
            : postInvoiceExtraction;
      const updated = await call(invoiceId, activeDocumentId);
      setExtraction(updated);
      await refreshDocuments();
      if (action === "post") {
        onInvoiceChanged();
      }
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  async function handleDelete(documentId: string) {
    setError(null);
    try {
      await deleteInvoiceDocument(invoiceId, documentId);
      if (activeDocumentId === documentId) {
        setExtraction(null);
        setActiveDocumentId("");
      }
      await refreshDocuments();
    } catch (err) {
      setError(err);
    }
  }

  function handleDownload(documentId: string) {
    const token = getAccessToken();
    fetch(invoiceDocumentContentUrl(invoiceId, documentId), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
      .then((res) => {
        if (!res.ok) throw new Error("Failed to download document");
        return res.blob();
      })
      .then((blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = `document-${documentId}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
      })
      .catch((err) => setError(err));
  }

  const visibleFields = (extraction?.fields ?? []).filter(
    (field) => Number(field.confidence) >= 0.7 || field.source === "MANUAL",
  );
  const terminal = extraction?.status === "POSTED" || extraction?.status === "REJECTED";

  return (
    <section aria-label="Invoice documents">
      <h3>Documents &amp; extraction</h3>
      <p className="muted">
        Upload a PDF, extract fields with provenance, review, then approve and post to this invoice.
        Extraction works offline; AI line ideas are optional and advisory.
      </p>

      <div>
        <input
          ref={fileInput}
          type="file"
          accept="application/pdf"
          aria-label="Upload invoice PDF"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) handleUpload(file);
            if (fileInput.current) fileInput.current.value = "";
          }}
          disabled={busy}
        />
      </div>

      {documents.length ? (
        <ul className="resource-list">
          {documents.map((doc) => (
            <li key={doc.documentId}>
              <strong>{doc.fileName}</strong>
              <p className="muted">
                {doc.status} · {(doc.sizeBytes / 1024).toFixed(0)} KB ·{" "}
                {new Date(doc.createdAt).toLocaleString()}
              </p>
              <div className="audit-job-actions">
                <button type="button" disabled={busy} onClick={() => handleExtract(doc.documentId)}>
                  Extract
                </button>
                {activeDocumentId !== doc.documentId ? (
                  <button type="button" onClick={() => handleViewExtraction(doc.documentId)}>
                    View
                  </button>
                ) : null}
                <button type="button" onClick={() => handleDownload(doc.documentId)}>
                  Download
                </button>
                <button type="button" onClick={() => handleDelete(doc.documentId)}>
                  Delete
                </button>
              </div>
            </li>
          ))}
        </ul>
      ) : loaded ? (
        <p className="muted">No documents yet. Upload a PDF above.</p>
      ) : null}

      {activeJob && (activeJob.status === "QUEUED" || activeJob.status === "RUNNING") ? (
        <p className="muted" role="status">
          Extraction {activeJob.status.toLowerCase()}… polling for completion.
        </p>
      ) : null}

      {extraction ? (
        <section aria-label="Extraction review">
          <h4>Review ({extraction.status})</h4>
          <p className="muted">
            Native pages: {extraction.methodNativePages} · OCR pages: {extraction.methodOcrPages} ·
            Extractor {extraction.extractorVersion}
          </p>
          {visibleFields.length ? (
            <table>
              <thead>
                <tr>
                  <th>Field</th>
                  <th>Value</th>
                  <th>Confidence</th>
                  <th>Source</th>
                  <th>Page</th>
                </tr>
              </thead>
              <tbody>
                {visibleFields.map((field) => (
                  <tr key={field.id} className={confidenceClass(Number(field.confidence))}>
                    <td>{field.fieldKey}</td>
                    <td>
                      {needsConfirmation(field) && !terminal ? (
                        <FieldEditor field={field} onConfirm={handleConfirm} />
                      ) : (
                        (field.fieldValue ?? field.moneyAmount ?? "—")
                      )}
                    </td>
                    <td>{Number(field.confidence).toFixed(2)}</td>
                    <td>{field.source}</td>
                    <td>{field.page ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="muted">No fields above the review threshold.</p>
          )}

          {extraction.status === "NEEDS_REVIEW" ? (
            <div className="audit-job-actions">
              <button type="button" disabled={busy} onClick={() => handleAction("approve")}>
                Approve
              </button>
              <button type="button" disabled={busy} onClick={() => handleAction("reject")}>
                Reject
              </button>
            </div>
          ) : null}
          {extraction.status === "APPROVED" ? (
            <div className="audit-job-actions">
              <button type="button" disabled={busy} onClick={() => handleAction("post")}>
                Post to invoice
              </button>
            </div>
          ) : null}
        </section>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
    </section>
  );
}

function FieldEditor({
  field,
  onConfirm,
}: {
  field: InvoiceField;
  onConfirm: (fieldId: string, value: string) => void;
}) {
  const [value, setValue] = useState(field.fieldValue ?? "");
  return (
    <span className="field-editor">
      <input
        aria-label={`Confirm ${field.fieldKey}`}
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
      <button type="button" onClick={() => onConfirm(field.id, value)}>
        Confirm
      </button>
      {field.rawSnippet ? <small className="muted">Evidence: {field.rawSnippet}</small> : null}
    </span>
  );
}