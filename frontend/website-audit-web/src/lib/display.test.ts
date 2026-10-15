import { describe, expect, it } from 'vitest';
import { failingChecks, isFailing, severityLabel } from './display';

describe('audit findings presentation', () => {
  it('treats only sub-100 checks as failing', () => {
    expect(isFailing(100)).toBe(false);
    expect(isFailing(99)).toBe(true);
    expect(isFailing(0)).toBe(true);
  });

  it('does not flag checks with missing or invalid scores', () => {
    expect(isFailing(null)).toBe(false);
    expect(isFailing(undefined)).toBe(false);
    expect(isFailing(NaN)).toBe(false);
  });

  it('filters a check list down to failing entries', () => {
    const checks = [{ score: 100 }, { score: 40 }, { score: 90 }, { score: 0 }];
    expect(failingChecks(checks).map((c) => c.score)).toEqual([40, 90, 0]);
    expect(failingChecks(null)).toEqual([]);
  });

  it('normalizes severity to a known badge label', () => {
    expect(severityLabel('high')).toBe('HIGH');
    expect(severityLabel('Medium')).toBe('MEDIUM');
    expect(severityLabel('weird')).toBe('INFO');
    expect(severityLabel(null)).toBe('INFO');
  });
});
