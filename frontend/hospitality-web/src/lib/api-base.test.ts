import { afterEach, describe, expect, it, vi } from 'vitest';
import { resolveApiBase } from './api-base';

describe('resolveApiBase', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('uses the configured absolute URL unchanged', () => {
    expect(resolveApiBase('https://api.hospitality.fixna.in/api')).toBe(
      'https://api.hospitality.fixna.in/api'
    );
  });

  it('strips trailing slashes', () => {
    expect(resolveApiBase('https://api.hospitality.fixna.in/api///')).toBe(
      'https://api.hospitality.fixna.in/api'
    );
  });

  it('repairs a schemeless host instead of letting the browser treat it as a path', () => {
    // This is the production misconfiguration that made login call
    // https://hospitality.fixna.in/fixna-hospitality.onrender.com/api/...
    expect(resolveApiBase('fixna-hospitality.onrender.com/api')).toBe(
      'https://fixna-hospitality.onrender.com/api'
    );
  });

  it('falls back to the local API when unset or blank', () => {
    expect(resolveApiBase(undefined)).toBe('http://localhost:8084/api');
    expect(resolveApiBase('')).toBe('http://localhost:8084/api');
    expect(resolveApiBase('   ')).toBe('http://localhost:8084/api');
  });

  it('trims surrounding whitespace', () => {
    expect(resolveApiBase('  http://localhost:8084/api  ')).toBe('http://localhost:8084/api');
  });
});
