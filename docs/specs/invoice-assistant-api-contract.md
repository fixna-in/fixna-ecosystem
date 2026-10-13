# API Contract — Invoice Assistant intake & review (proposed)

Base `/api/v1`; `ApiError`/`FixnaException` conventions; JWT required; tenant server-derived; cross-tenant → 404. Shipped suggestion endpoints (`/invoices/{id}/assist/*`, `/assist/suggestions/{id}/*`) are unchanged.

## Upload — `POST /invoices/{id}/documents` (multipart, `INVOICE_MANAGE`) → 201

```json
{ "documentId": "uuid", "status": "RECEIVED", "fileName": "invoice-4711.pdf",
  "sizeBytes": 183204, "links": { "extract": "/invoices/{id}/documents/{documentId}/extract" } }
```
Errors: `404 INVOICE_NOT_FOUND`, `413 FILE_TOO_LARGE`, `415 UNSUPPORTED_MEDIA_TYPE`, `422 INVALID_FILE` (magic-byte mismatch), `429` rate-limited.

## List / download / delete

- `GET /invoices/{id}/documents` → 200 list `{documentId, fileName, sizeBytes, status, createdAt}`.
- `GET /invoices/{id}/documents/{documentId}/content` → 200 `application/pdf` (streamed via `FileStorage.open`); `404` cross-tenant.
- `DELETE /invoices/{id}/documents/{documentId}` → 204; hard delete + tombstone audit.

## Extract — `POST /invoices/{id}/documents/{documentId}/extract`

First iteration may run synchronously (≤ 30s); target returns 202 with job conventions from the Website Audit contract (same in-process worker pattern). Completed response:

```json
{
  "extractionId": "uuid", "status": "NEEDS_REVIEW",
  "method": { "nativeTextPages": 2, "ocrPages": 0 },
  "fields": [
    { "key": "total", "value": "1234.56", "currency": "EUR", "confidence": 0.96,
      "provenance": { "source": "NATIVE_TEXT", "page": 2, "bbox": [72, 880, 300, 910],
                      "rawSnippet": "Gesamtbetrag 1.234,56 EUR" } }
  ]
}
```
Errors: `409 EXTRACTION_IN_PROGRESS`, `503 OCR_UNAVAILABLE` (no native text and OCR engine down), `422 NO_TEXT_FOUND`.

## Review (all `INVOICE_MANAGE`)

- `GET .../extraction` → current extraction + fields (incl. provenance, confidence).
- `PATCH .../extraction/fields` body `[{key, value}]` → 200; edits record source `MANUAL` + actor user id.
- `POST .../extraction/approve` → 200 `APPROVED`; `409 GATES_UNMET` listing the blocking field keys.
- `POST .../extraction/reject` → 200 `REJECTED` (reason optional, audited).
- `POST .../extraction/post` → 200 `POSTED` + invoice line diff summary; `409 INVOICE_NOT_EDITABLE` when the invoice is not `DRAFT`; `409 INVALID_STATE` for illegal transitions.

## Export

`GET .../extraction/export?format=csv|json` → 200 attachment `invoice-{id}-extraction.csv|json`; action audited.

## Error code registry (new codes this design introduces)

`INVALID_FILE`, `UNSUPPORTED_MEDIA_TYPE`, `FILE_TOO_LARGE`, `EXTRACTION_IN_PROGRESS`, `NO_TEXT_FOUND`, `OCR_UNAVAILABLE`, `GATES_UNMET`, `INVALID_STATE`, `INVOICE_NOT_EDITABLE`, `AI_UNAVAILABLE`.

All codes map to 4xx except `OCR_UNAVAILABLE`/`AI_UNAVAILABLE` (503) and unexpected failures (500 generic `ApiError`, never leaking internals).
