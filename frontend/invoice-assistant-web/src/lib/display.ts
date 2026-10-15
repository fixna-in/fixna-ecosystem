/** Confidence is 0..1; the backend gates 0.90 auto / 0.70 review / <0.70 hidden. */
export function confidenceTier(confidence: number | string | null | undefined): {
  tier: 'HIGH' | 'REVIEW' | 'LOW' | 'UNKNOWN';
  label: string;
} {
  // Missing/blank confidence must stay distinct from a measured 0.0.
  if (confidence == null) return { tier: 'UNKNOWN', label: 'Unknown' };
  if (typeof confidence === 'string' && confidence.trim() === '') {
    return { tier: 'UNKNOWN', label: 'Unknown' };
  }
  const numeric = Number(confidence);
  if (!Number.isFinite(numeric)) {
    return { tier: 'UNKNOWN', label: 'Unknown' };
  }
  if (numeric >= 0.9) return { tier: 'HIGH', label: 'High' };
  if (numeric >= 0.7) return { tier: 'REVIEW', label: 'Review' };
  return { tier: 'LOW', label: 'Low' };
}

/** Money is stored as an exact decimal string/number; never re-format into float. */
export function formatMoney(
  amount: string | number | null | undefined,
  currency?: string | null
): string {
  if (amount == null || amount === '') return '—';
  const numeric = Number(amount);
  if (!Number.isFinite(numeric)) return String(amount);
  const formatted = numeric.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return currency ? `${currency} ${formatted}` : formatted;
}

export function formatBytes(bytes: number | null | undefined): string {
  if (bytes == null || !Number.isFinite(bytes) || bytes < 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
