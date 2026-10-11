"use client";

import { useState } from "react";

type FormState =
  | { kind: "idle" }
  | { kind: "submitting" }
  | { kind: "success"; message: string }
  | { kind: "error"; message: string; fieldErrors?: Record<string, string> };

const FIELDS = [
  { id: "name", label: "Name", type: "text", autoComplete: "name", required: true },
  { id: "company", label: "Company", type: "text", autoComplete: "organization", required: false },
  { id: "email", label: "Email", type: "email", autoComplete: "email", required: true },
  { id: "phone", label: "Phone (optional)", type: "tel", autoComplete: "tel", required: false },
] as const;

export function ContactForm() {
  const [state, setState] = useState<FormState>({ kind: "idle" });
  const [honeypot, setHoneypot] = useState("");

  const submitting = state.kind === "submitting";

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setState({ kind: "submitting" });

    try {
      const response = await fetch("/api/contact", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: data.get("name"),
          company: data.get("company"),
          email: data.get("email"),
          phone: data.get("phone"),
          requirement: data.get("requirement"),
          companyWebsite: honeypot,
        }),
      });
      const payload = (await response.json().catch(() => ({}))) as {
        ok?: boolean;
        message?: string;
        errors?: Record<string, string>;
      };
      if (response.ok && payload.ok) {
        setState({ kind: "success", message: payload.message ?? "Your enquiry has been recorded." });
        form.reset();
        return;
      }
      setState({
        kind: "error",
        message: payload.message ?? "Something went wrong. Please try again or email hello@fixna.in.",
        fieldErrors: payload.errors,
      });
    } catch {
      setState({
        kind: "error",
        message: "We could not reach the server. Please try again or email hello@fixna.in.",
      });
    }
  }

  if (state.kind === "success") {
    return (
      <div className="contact-success" role="status">
        <h2>Enquiry recorded</h2>
        <p>{state.message}</p>
        <p className="form-note">
          Need to add something? Email <a href="mailto:hello@fixna.in">hello@fixna.in</a>.
        </p>
      </div>
    );
  }

  return (
    <form className="contact-form" onSubmit={handleSubmit} noValidate>
      {FIELDS.map((field) => (
        <div className="field" key={field.id}>
          <label htmlFor={field.id}>{field.label}</label>
          <input
            id={field.id}
            name={field.id}
            type={field.type}
            autoComplete={field.autoComplete}
            required={field.required}
            aria-invalid={Boolean(state.kind === "error" && state.fieldErrors?.[field.id])}
          />
          {state.kind === "error" && state.fieldErrors?.[field.id] ? (
            <p className="field-error" role="alert">
              {state.fieldErrors[field.id]}
            </p>
          ) : null}
        </div>
      ))}
      <div className="field">
        <label htmlFor="requirement">Requirement</label>
        <textarea
          id="requirement"
          name="requirement"
          required
          aria-invalid={Boolean(state.kind === "error" && state.fieldErrors?.["requirement"])}
          placeholder="Tell us what you are building, modernizing, or automating."
        />
        {state.kind === "error" && state.fieldErrors?.["requirement"] ? (
          <p className="field-error" role="alert">
            {state.fieldErrors["requirement"]}
          </p>
        ) : null}
      </div>
      {/* Honeypot — hidden from real users; bots fill it and are dropped. */}
      <div className="hp-field" aria-hidden="true">
        <label htmlFor="companyWebsite">Company website</label>
        <input
          id="companyWebsite"
          name="companyWebsite"
          type="text"
          tabIndex={-1}
          autoComplete="off"
          value={honeypot}
          onChange={(event) => setHoneypot(event.target.value)}
        />
      </div>
      <button className="button button-primary" type="submit" disabled={submitting}>
        {submitting ? "Sending…" : "Send Requirement"}
      </button>
      {state.kind === "error" ? (
        <p className="field-error" role="alert">
          {state.message}
        </p>
      ) : null}
      <p className="form-note">
        Your enquiry is recorded securely and we reply by email. Prefer email?{" "}
        <a href="mailto:hello@fixna.in">hello@fixna.in</a>.
      </p>
    </form>
  );
}
