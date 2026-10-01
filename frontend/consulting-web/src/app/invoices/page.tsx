"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { listClients } from "@/lib/clients-api";
import {
  addInvoiceLine,
  cancelInvoice,
  createInvoice,
  getInvoice,
  listInvoices,
  listPayments,
  recordPayment,
  sendInvoice,
  InvoiceInputSchema,
  InvoiceLineInputSchema,
  PaymentInputSchema,
  type InvoiceInput,
  type InvoiceLineInput,
  type PaymentInput,
} from "@/lib/invoices-api";
import { listProposals } from "@/lib/proposals-api";
import { ErrorState, LoadingState } from "../providers";

export default function InvoicesPage() {
  const [error, setError] = useState<unknown>(null);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedInvoiceId, setSelectedInvoiceId] = useState("");

  const clients = useQuery({ queryKey: ["clients"], queryFn: listClients, retry: false });
  const proposals = useQuery({
    queryKey: ["proposals", selectedClientId],
    queryFn: () => listProposals(selectedClientId),
    enabled: Boolean(selectedClientId),
    retry: false,
  });
  const invoices = useQuery({
    queryKey: ["invoices", selectedClientId],
    queryFn: () => listInvoices(selectedClientId),
    enabled: Boolean(selectedClientId),
    retry: false,
  });
  const invoiceDetail = useQuery({
    queryKey: ["invoice", selectedInvoiceId],
    queryFn: () => getInvoice(selectedInvoiceId),
    enabled: Boolean(selectedInvoiceId),
    retry: false,
  });
  const invoicePayments = useQuery({
    queryKey: ["invoice-payments", selectedInvoiceId],
    queryFn: () => listPayments(selectedInvoiceId),
    enabled: Boolean(selectedInvoiceId),
    retry: false,
  });

  const invoiceForm = useForm<InvoiceInput>({
    resolver: zodResolver(InvoiceInputSchema),
    defaultValues: { clientId: "", title: "", currency: "USD" },
  });
  const lineForm = useForm<InvoiceLineInput>({
    resolver: zodResolver(InvoiceLineInputSchema),
    defaultValues: { description: "", quantity: 1, unitPrice: 0, sortOrder: 0 },
  });
  const paymentForm = useForm<PaymentInput>({
    resolver: zodResolver(PaymentInputSchema),
    defaultValues: { amount: 0, paymentMethod: "bank_transfer", reference: "" },
  });

  async function refreshAll() {
    await Promise.all([
      clients.refetch(),
      proposals.refetch(),
      invoices.refetch(),
      invoiceDetail.refetch(),
      invoicePayments.refetch(),
    ]);
  }

  const selectedInvoice = invoiceDetail.data;
  const approvedProposals = proposals.data?.filter((proposal) => proposal.status === "APPROVED") ?? [];

  return (
    <section aria-label="Invoices">
      <h1>Invoices</h1>
      <p className="muted">Create draft invoices, add line items, send, and record payments.</p>

      <label>
        Client
        <select
          value={selectedClientId}
          onChange={(event) => {
            setSelectedClientId(event.target.value);
            setSelectedInvoiceId("");
            invoiceForm.setValue("clientId", event.target.value);
          }}
        >
          <option value="">Select a client</option>
          {clients.data?.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
            </option>
          ))}
        </select>
      </label>

      {selectedClientId ? (
        <form
          onSubmit={invoiceForm.handleSubmit(async (values) => {
            setError(null);
            try {
              const invoice = await createInvoice({ ...values, clientId: selectedClientId });
              invoiceForm.reset({ clientId: selectedClientId, title: "", currency: "USD" });
              setSelectedInvoiceId(invoice.id);
              await refreshAll();
            } catch (err) {
              setError(err);
            }
          })}
        >
          <h2>Create draft invoice</h2>
          <label>
            Title
            <input {...invoiceForm.register("title")} />
          </label>
          {invoiceForm.formState.errors.title ? (
            <p role="alert">{invoiceForm.formState.errors.title.message}</p>
          ) : null}
          <label>
            Due date
            <input type="date" {...invoiceForm.register("dueDate")} />
          </label>
          <label>
            From approved proposal (optional)
            <select {...invoiceForm.register("proposalId")}>
              <option value="">None</option>
              {approvedProposals.map((proposal) => (
                <option key={proposal.id} value={proposal.id}>
                  {proposal.title} — {proposal.currency} {proposal.totalAmount}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" disabled={invoiceForm.formState.isSubmitting}>
            {invoiceForm.formState.isSubmitting ? "Creating…" : "Add invoice"}
          </button>
        </form>
      ) : null}

      {invoices.isLoading && selectedClientId ? <LoadingState label="Loading invoices" /> : null}

      {invoices.data?.length ? (
        <section>
          <h2>Invoices</h2>
          <ul className="resource-list">
            {invoices.data.map((invoice) => (
              <li key={invoice.id}>
                <strong>
                  {invoice.invoiceNumber} — {invoice.title}
                </strong>
                <p className="muted">
                  Status: {invoice.status} · Total: {invoice.currency} {invoice.totalAmount} · Paid:{" "}
                  {invoice.currency} {invoice.amountPaid}
                </p>
                <button type="button" onClick={() => setSelectedInvoiceId(invoice.id)}>
                  {selectedInvoiceId === invoice.id ? "Selected" : "Manage"}
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : selectedClientId ? (
        <p className="muted">No invoices yet.</p>
      ) : null}

      {selectedInvoice ? (
        <section>
          <h2>Invoice details</h2>
          <p className="muted">
            {selectedInvoice.invoiceNumber} — {selectedInvoice.title} — {selectedInvoice.status} · Subtotal{" "}
            {selectedInvoice.currency} {selectedInvoice.subtotal} · Total {selectedInvoice.currency}{" "}
            {selectedInvoice.totalAmount} · Paid {selectedInvoice.currency} {selectedInvoice.amountPaid}
          </p>

          {selectedInvoice.lines.length ? (
            <ul className="resource-list">
              {selectedInvoice.lines.map((line) => (
                <li key={line.id}>
                  {line.description} — qty {line.quantity} × {line.unitPrice} = {line.lineTotal}
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">No line items yet.</p>
          )}

          {selectedInvoice.status === "DRAFT" ? (
            <>
              <form
                onSubmit={lineForm.handleSubmit(async (values) => {
                  setError(null);
                  try {
                    await addInvoiceLine(selectedInvoice.id, values);
                    lineForm.reset({ description: "", quantity: 1, unitPrice: 0, sortOrder: 0 });
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                })}
              >
                <h3>Add line item</h3>
                <label>
                  Description
                  <input {...lineForm.register("description")} />
                </label>
                <label>
                  Quantity
                  <input type="number" step="0.01" {...lineForm.register("quantity")} />
                </label>
                <label>
                  Unit price
                  <input type="number" step="0.01" {...lineForm.register("unitPrice")} />
                </label>
                <button type="submit" disabled={lineForm.formState.isSubmitting}>
                  {lineForm.formState.isSubmitting ? "Adding…" : "Add item"}
                </button>
              </form>

              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await sendInvoice(selectedInvoice.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Send invoice
              </button>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await cancelInvoice(selectedInvoice.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Cancel
              </button>
            </>
          ) : null}

          {selectedInvoice.status === "SENT" || selectedInvoice.status === "PARTIALLY_PAID" ? (
            <>
              <form
                onSubmit={paymentForm.handleSubmit(async (values) => {
                  setError(null);
                  try {
                    await recordPayment(selectedInvoice.id, values);
                    paymentForm.reset({ amount: 0, paymentMethod: "bank_transfer", reference: "" });
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                })}
              >
                <h3>Record payment</h3>
                <label>
                  Amount
                  <input type="number" step="0.01" {...paymentForm.register("amount")} />
                </label>
                <label>
                  Payment method
                  <input {...paymentForm.register("paymentMethod")} />
                </label>
                <label>
                  Reference
                  <input {...paymentForm.register("reference")} />
                </label>
                <button type="submit" disabled={paymentForm.formState.isSubmitting}>
                  {paymentForm.formState.isSubmitting ? "Recording…" : "Record payment"}
                </button>
              </form>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await cancelInvoice(selectedInvoice.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Cancel invoice
              </button>
            </>
          ) : null}

          {invoicePayments.data?.length ? (
            <section>
              <h3>Payments</h3>
              <ul className="resource-list">
                {invoicePayments.data.map((payment) => (
                  <li key={payment.id}>
                    {payment.currency} {payment.amount} via {payment.paymentMethod}
                    {payment.reference ? ` (${payment.reference})` : ""} — {payment.paidAt}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </section>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
      {clients.error ? <ErrorState error={clients.error} /> : null}
      {proposals.error ? <ErrorState error={proposals.error} /> : null}
      {invoices.error ? <ErrorState error={invoices.error} /> : null}
      {invoiceDetail.error ? <ErrorState error={invoiceDetail.error} /> : null}
      {invoicePayments.error ? <ErrorState error={invoicePayments.error} /> : null}
    </section>
  );
}
