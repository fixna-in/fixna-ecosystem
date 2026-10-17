/**
 * Single source of truth for the API base URL.
 *
 * `NEXT_PUBLIC_API_BASE_URL` is baked into the bundle at build time. If the
 * deployed value is missing its `http(s)://` scheme (e.g.
 * `fixna-hospitality.onrender.com/api`), the browser treats it as a *path* on
 * the web origin and every request silently goes to the wrong server
 * (`https://<web-host>/<api-host>/api/...` → 400). Normalize the value here so
 * a schemeless host is repaired instead of breaking auth.
 */
const FALLBACK = 'http://localhost:8084/api';

export function resolveApiBase(raw: string | undefined): string {
  const trimmed = (raw ?? '').trim();
  if (!trimmed) {
    return FALLBACK;
  }
  const withoutTrailingSlash = trimmed.replace(/\/+$/, '');
  if (/^https?:\/\//i.test(withoutTrailingSlash)) {
    return withoutTrailingSlash;
  }
  if (process.env.NODE_ENV !== 'production') {
    // eslint-disable-next-line no-console
    console.warn(
      `[api-base] NEXT_PUBLIC_API_BASE_URL is missing its scheme (value: "${trimmed}"). ` +
        `Prepending https:// — set the full URL (e.g. https://api.hospitality.fixna.in/api) to silence this warning.`
    );
  }
  return `https://${withoutTrailingSlash}`;
}

export const API_BASE: string = resolveApiBase(process.env.NEXT_PUBLIC_API_BASE_URL);
