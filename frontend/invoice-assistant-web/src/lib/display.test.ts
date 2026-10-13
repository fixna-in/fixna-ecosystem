import { describe, expect, it } from 'vitest';
import { confidenceTier, formatBytes, formatMoney } from './display';

describe('invoice presentation', () => {
  it('maps confidence onto the backend gate tiers', () => {
    expect(confidenceTier(0.95).tier).toBe('HIGH');
    expect(confidenceTier(0.9).tier).toBe('HIGH');
    expect(confidenceTier(0.89).tier).toBe('REVIEW');
    expect(confidenceTier(0.7).tier).toBe('REVIEW');
    expect(confidenceTier(0.69).tier).toBe('LOW');
    expect(confidenceTier(0).tier).toBe('LOW');
  });

  it('treats missing or non-numeric confidence as unknown', () => {
    expect(confidenceTier(null).tier).toBe('UNKNOWN');
    expect(confidenceTier(undefined).tier).toBe('UNKNOWN');
    expect(confidenceTier('').tier).toBe('UNKNOWN');
    expect(confidenceTier('   ').tier).toBe('UNKNOWN');
    expect(confidenceTier('n/a').tier).toBe('UNKNOWN');
    expect(confidenceTier(NaN).tier).toBe('UNKNOWN');
  });

  it('renders money without converting exact decimals to float', () => {
    expect(formatMoney('1180.00', 'INR')).toBe('INR 1,180.00');
    expect(formatMoney(1000, 'USD')).toBe('USD 1,000.00');
    expect(formatMoney(null)).toBe('—');
  });

  it('formats byte sizes for the document list', () => {
    expect(formatBytes(512)).toBe('512 B');
    expect(formatBytes(2048)).toBe('2.0 KB');
    expect(formatBytes(5 * 1024 * 1024)).toBe('5.0 MB');
    expect(formatBytes(null)).toBe('—');
  });
});
