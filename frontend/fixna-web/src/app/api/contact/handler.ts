/**
 * Contact form handler — pure logic, dependency-injected, fully unit-testable.
 *
 * A submission is only ever claimed as "recorded" (it is appended to the
 * enquiry sheet), never as "emailed". Failure modes map to the standard
 * outcomes: 400 validation, 429 rate limit, 503 misconfiguration/unavailable,
 * 502 storage failure. No internal error details leave the server.
 */

export type ContactSubmission = {
  name: string;
  company: string;
  email: string;
  phone: string;
  requirement: string;
};

export type ValidationResult =
  | { ok: true; value: ContactSubmission }
  | { ok: false; errors: Record<string, string> };

/**
 * Honeypot trap field name — deliberately obscure so browser autofill and
 * password managers do not recognise or fill it (the previous
 * `companyWebsite` name was autofilled as a URL field, discarding real
 * enquiries as suspected bots). The value is read from FormData at submit
 * time and never stored. Keep in sync with `contact-form.tsx` via the
 * exported constant — do not rename one side only.
 */
export const CONTACT_HONEYPOT_FIELD = "fx_hp_confirm_9q2";

const LIMITS = {
  name: 120,
  company: 200,
  email: 320,
  phone: 40,
  requirement: 5000,
} as const;

// Pragmatic email shape check (full RFC validity is not the goal here; the
// address is only used to reply to the enquiry).
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function asTrimmedString(value: unknown): string {
  return typeof value === "string" ? value.trim() : "";
}

export function validateContact(body: unknown): ValidationResult {
  const errors: Record<string, string> = {};
  const raw = (body ?? {}) as Record<string, unknown>;

  const name = asTrimmedString(raw.name);
  const company = asTrimmedString(raw.company);
  const email = asTrimmedString(raw.email);
  const phone = asTrimmedString(raw.phone);
  const requirement = asTrimmedString(raw.requirement);

  if (!name) errors.name = "Name is required.";
  else if (name.length > LIMITS.name) errors.name = `Name must be at most ${LIMITS.name} characters.`;

  if (company.length > LIMITS.company) {
    errors.company = `Company must be at most ${LIMITS.company} characters.`;
  }

  if (!email) errors.email = "Email is required.";
  else if (email.length > LIMITS.email) {
    errors.email = `Email must be at most ${LIMITS.email} characters.`;
  } else if (!EMAIL_RE.test(email)) {
    errors.email = "Enter a valid email address.";
  }

  if (phone.length > LIMITS.phone) {
    errors.phone = `Phone must be at most ${LIMITS.phone} characters.`;
  } else if (phone && !/^[+\d][\d\s\-().]*$/.test(phone)) {
    errors.phone = "Enter a valid phone number.";
  }

  if (!requirement) errors.requirement = "Tell us what you are building.";
  else if (requirement.length > LIMITS.requirement) {
    errors.requirement = `Requirement must be at most ${LIMITS.requirement} characters.`;
  }

  if (Object.keys(errors).length > 0) return { ok: false, errors };
  return { ok: true, value: { name, company, email, phone, requirement } };
}

/** Row layout written to the enquiry sheet — keep in sync with docs. */
export function toSheetRow(value: ContactSubmission, receivedAt: Date): string[] {
  return [
    receivedAt.toISOString(),
    value.name,
    value.company,
    value.email,
    value.phone,
    value.requirement,
  ];
}

export type ContactHandlerDeps = {
  /** Appends one enquiry row to durable storage. Throws on failure. */
  appendRow: (row: string[]) => Promise<void>;
  /** False when storage is not configured (env vars missing). */
  isConfigured: () => boolean;
  /** Sliding-window rate limiter hook; returns false when over quota. */
  consumeRateLimit?: (clientKey: string) => boolean;
  now?: () => Date;
};

const JSON_HEADERS = { "Content-Type": "application/json" };

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: JSON_HEADERS });
}

export function createContactHandler(deps: ContactHandlerDeps) {
  const now = deps.now ?? (() => new Date());

  return async function handle(request: Request): Promise<Response> {
    if (request.method !== "POST") {
      return json(405, { ok: false, code: "METHOD_NOT_ALLOWED" });
    }

    if (!deps.isConfigured()) {
      // Misconfiguration must not leak internals; point visitors at email.
      return json(503, {
        ok: false,
        code: "FORM_UNAVAILABLE",
        message: "The contact form is temporarily unavailable. Please email fixnainfo@gmail.com.",
      });
    }

    const clientKey =
      request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() ||
      request.headers.get("x-real-ip") ||
      "unknown";
    if (deps.consumeRateLimit && !deps.consumeRateLimit(clientKey)) {
      return json(429, {
        ok: false,
        code: "RATE_LIMIT_EXCEEDED",
        message: "Too many submissions — please retry shortly or email fixnainfo@gmail.com.",
      });
    }

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return json(400, { ok: false, code: "INVALID_JSON", message: "Request body must be JSON." });
    }

    // Honeypot: bots fill hidden fields. Pretend success, store nothing.
    // The key living on CONTACT_HONEYPOT_FIELD avoids browser autofill
    // false-positives; legacy `companyWebsite` keys are plain fields now.
    const honeypot = (body as Record<string, unknown>)?.[CONTACT_HONEYPOT_FIELD];
    if (typeof honeypot === "string" && honeypot.trim() !== "") {
      // eslint-disable-next-line no-console
      console.log("contact:honeypot trap");
      return json(202, { ok: true, message: "Thanks — your enquiry has been recorded." });
    }

    const result = validateContact(body);
    if (!result.ok) {
      return json(400, { ok: false, code: "VALIDATION_FAILED", errors: result.errors });
    }

    try {
      await deps.appendRow(toSheetRow(result.value, now()));
    } catch {
      return json(502, {
        ok: false,
        code: "STORAGE_FAILED",
        message: "We could not record your enquiry right now. Please email fixnainfo@gmail.com.",
      });
    }

    return json(202, {
      ok: true,
      message: `Thanks ${result.value.name} — your enquiry has been recorded. We will reply to ${result.value.email}.`,
    });
  };
}

/**
 * Fixed-window per-IP limiter for the form (in-memory; per serverless
 * instance). Sufficient as abuse protection for an MVP contact form; a
 * shared store (e.g. Redis/KV) is the documented upgrade path.
 */
export function createInMemoryRateLimiter(limit: number, windowMs: number) {
  const hits = new Map<string, { count: number; resetAt: number }>();
  return (key: string): boolean => {
    const nowMs = Date.now();
    if (hits.size > 10_000) {
      for (const [k, v] of hits) if (v.resetAt <= nowMs) hits.delete(k);
    }
    const entry = hits.get(key);
    if (!entry || entry.resetAt <= nowMs) {
      hits.set(key, { count: 1, resetAt: nowMs + windowMs });
      return true;
    }
    if (entry.count >= limit) return false;
    entry.count += 1;
    return true;
  };
}
