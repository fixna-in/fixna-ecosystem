const DEFAULT_SITE_URL = "http://localhost:3002";

export function resolveSiteUrl(value: string | undefined): string {
  const rawValue = value?.trim();

  if (!rawValue) {
    return DEFAULT_SITE_URL;
  }

  if (/^https?:\/\//i.test(rawValue)) {
    try {
      return new URL(rawValue).toString().replace(/\/$/, "");
    } catch {
      return DEFAULT_SITE_URL;
    }
  }

  if (/^[a-z0-9.-]+\.[a-z]{2,}(?::\d+)?$/i.test(rawValue)) {
    return `https://${rawValue}`;
  }

  return DEFAULT_SITE_URL;
}

export const siteUrl = resolveSiteUrl(process.env.NEXT_PUBLIC_SITE_URL);
