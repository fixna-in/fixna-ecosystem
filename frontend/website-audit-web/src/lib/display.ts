/** Audit checks carry a 0–100 score; a check "fails" below full marks. */
export function isFailing(score: number | null | undefined): boolean {
  if (score == null || !Number.isFinite(score)) return false;
  return score < 100;
}

export function failingChecks<T extends { score: number }>(checks: T[] | null | undefined): T[] {
  if (!checks) return [];
  return checks.filter((c) => isFailing(c.score));
}

/** Severity is normalized to an uppercase badge label. */
export function severityLabel(severity: string | null | undefined): string {
  if (!severity) return 'INFO';
  const upper = severity.toUpperCase();
  return ['HIGH', 'MEDIUM', 'LOW', 'INFO'].includes(upper) ? upper : 'INFO';
}
