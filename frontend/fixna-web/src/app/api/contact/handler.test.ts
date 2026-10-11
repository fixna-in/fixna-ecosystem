import { describe, expect, it } from "vitest";
import {
  createContactHandler,
  createInMemoryRateLimiter,
  toSheetRow,
  validateContact,
  type ContactHandlerDeps,
} from "./handler";

const VALID_BODY = {
  name: "Test Person",
  company: "Acme",
  email: "test@example.com",
  phone: "+91 98765 43210",
  requirement: "We need a Kafka-based integration platform.",
};

function post(body: unknown, headers: Record<string, string> = {}): Request {
  return new Request("http://localhost/api/contact", {
    method: "POST",
    headers: { "Content-Type": "application/json", ...headers },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

function deps(overrides: Partial<ContactHandlerDeps> = {}): ContactHandlerDeps {
  return {
    appendRow: async () => {},
    isConfigured: () => true,
    ...overrides,
  };
}

describe("validateContact", () => {
  it("accepts a valid submission", () => {
    const result = validateContact(VALID_BODY);
    expect(result.ok).toBe(true);
  });

  it("requires name, email, and requirement", () => {
    const result = validateContact({});
    expect(result.ok).toBe(false);
    if (!result.ok) {
      expect(Object.keys(result.errors).sort()).toEqual(["email", "name", "requirement"]);
    }
  });

  it("rejects malformed email", () => {
    const result = validateContact({ ...VALID_BODY, email: "not-an-email" });
    expect(result.ok).toBe(false);
  });

  it("rejects malformed phone", () => {
    const result = validateContact({ ...VALID_BODY, phone: "call me maybe" });
    expect(result.ok).toBe(false);
  });

  it("enforces length limits", () => {
    const result = validateContact({ ...VALID_BODY, requirement: "x".repeat(5001) });
    expect(result.ok).toBe(false);
  });

  it("trims inputs", () => {
    const result = validateContact({ ...VALID_BODY, name: "  Ada  " });
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.value.name).toBe("Ada");
  });
});

describe("toSheetRow", () => {
  it("writes timestamp first, then fields in order", () => {
    const when = new Date("2026-10-09T10:00:00.000Z");
    const result = validateContact(VALID_BODY);
    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(toSheetRow(result.value, when)).toEqual([
        "2026-10-09T10:00:00.000Z",
        "Test Person",
        "Acme",
        "test@example.com",
        "+91 98765 43210",
        "We need a Kafka-based integration platform.",
      ]);
    }
  });
});

describe("createContactHandler", () => {
  it("records a valid submission and confirms it", async () => {
    const rows: string[][] = [];
    const handle = createContactHandler(deps({ appendRow: async (row) => { rows.push(row); } }));
    const response = await handle(post(VALID_BODY));
    expect(response.status).toBe(202);
    const payload = await response.json();
    expect(payload.ok).toBe(true);
    expect(payload.message).toContain("test@example.com");
    expect(rows).toHaveLength(1);
  });

  it("rejects invalid submissions without storing", async () => {
    let called = false;
    const handle = createContactHandler(deps({ appendRow: async () => { called = true; } }));
    const response = await handle(post({ ...VALID_BODY, email: "nope" }));
    expect(response.status).toBe(400);
    const payload = await response.json();
    expect(payload.code).toBe("VALIDATION_FAILED");
    expect(payload.errors.email).toBeDefined();
    expect(called).toBe(false);
  });

  it("rate limits repeated submissions per client", async () => {
    let count = 0;
    const handle = createContactHandler(
      deps({ consumeRateLimit: () => (count++ < 2) }),
    );
    expect((await handle(post(VALID_BODY))).status).toBe(202);
    expect((await handle(post(VALID_BODY))).status).toBe(202);
    expect((await handle(post(VALID_BODY))).status).toBe(429);
  });

  it("pretends success for honeypot submissions without storing", async () => {
    let called = false;
    const handle = createContactHandler(deps({ appendRow: async () => { called = true; } }));
    const response = await handle(post({ ...VALID_BODY, companyWebsite: "spam.example" }));
    expect(response.status).toBe(202);
    expect(called).toBe(false);
  });

  it("returns a generic error when storage fails", async () => {
    const handle = createContactHandler(
      deps({ appendRow: async () => { throw new Error("secret internal detail"); } }),
    );
    const response = await handle(post(VALID_BODY));
    expect(response.status).toBe(502);
    const text = await response.text();
    expect(text).not.toContain("secret internal detail");
  });

  it("returns 503 when storage is not configured", async () => {
    const handle = createContactHandler(deps({ isConfigured: () => false }));
    const response = await handle(post(VALID_BODY));
    expect(response.status).toBe(503);
    const payload = await response.json();
    expect(payload.message).toContain("hello@fixna.in");
  });

  it("rejects non-POST methods", async () => {
    const handle = createContactHandler(deps());
    const response = await handle(new Request("http://localhost/api/contact", { method: "GET" }));
    expect(response.status).toBe(405);
  });

  it("rejects malformed JSON bodies", async () => {
    const handle = createContactHandler(deps());
    const response = await handle(post("{not json"));
    expect(response.status).toBe(400);
  });
});

describe("createInMemoryRateLimiter", () => {
  it("allows up to the limit then blocks within the window", () => {
    const limit = createInMemoryRateLimiter(2, 60_000);
    expect(limit("ip1")).toBe(true);
    expect(limit("ip1")).toBe(true);
    expect(limit("ip1")).toBe(false);
  });

  it("isolates keys", () => {
    const limit = createInMemoryRateLimiter(1, 60_000);
    expect(limit("a")).toBe(true);
    expect(limit("b")).toBe(true);
  });
});
