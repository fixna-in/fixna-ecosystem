"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { listClients } from "@/lib/clients-api";
import {
  addProposalItem,
  approveProposal,
  cancelProposal,
  createProposal,
  getProposal,
  listProposals,
  ProposalInputSchema,
  ProposalItemInputSchema,
  rejectProposal,
  sendProposal,
  type ProposalInput,
  type ProposalItemInput,
} from "@/lib/proposals-api";
import { ErrorState, LoadingState } from "../providers";

export default function ProposalsPage() {
  const [error, setError] = useState<unknown>(null);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedProposalId, setSelectedProposalId] = useState("");

  const clients = useQuery({ queryKey: ["clients"], queryFn: listClients, retry: false });
  const proposals = useQuery({
    queryKey: ["proposals", selectedClientId],
    queryFn: () => listProposals(selectedClientId),
    enabled: Boolean(selectedClientId),
    retry: false,
  });
  const proposalDetail = useQuery({
    queryKey: ["proposal", selectedProposalId],
    queryFn: () => getProposal(selectedProposalId),
    enabled: Boolean(selectedProposalId),
    retry: false,
  });

  const proposalForm = useForm<ProposalInput>({
    resolver: zodResolver(ProposalInputSchema),
    defaultValues: { clientId: "", title: "", currency: "USD" },
  });
  const itemForm = useForm<ProposalItemInput>({
    resolver: zodResolver(ProposalItemInputSchema),
    defaultValues: { description: "", quantity: 1, unitPrice: 0, sortOrder: 0 },
  });

  async function refreshAll() {
    await Promise.all([clients.refetch(), proposals.refetch(), proposalDetail.refetch()]);
  }

  const selectedProposal = proposalDetail.data;

  return (
    <section aria-label="Proposals">
      <h1>Proposals</h1>
      <p className="muted">Create draft proposals, add line items, send, and manage approval.</p>

      <label>
        Client
        <select
          value={selectedClientId}
          onChange={(event) => {
            setSelectedClientId(event.target.value);
            setSelectedProposalId("");
            proposalForm.setValue("clientId", event.target.value);
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
          onSubmit={proposalForm.handleSubmit(async (values) => {
            setError(null);
            try {
              const proposal = await createProposal({ ...values, clientId: selectedClientId });
              proposalForm.reset({ clientId: selectedClientId, title: "", currency: "USD" });
              setSelectedProposalId(proposal.id);
              await refreshAll();
            } catch (err) {
              setError(err);
            }
          })}
        >
          <h2>Create draft proposal</h2>
          <label>
            Title
            <input {...proposalForm.register("title")} />
          </label>
          {proposalForm.formState.errors.title ? (
            <p role="alert">{proposalForm.formState.errors.title.message}</p>
          ) : null}
          <label>
            Description
            <textarea {...proposalForm.register("description")} />
          </label>
          <button type="submit" disabled={proposalForm.formState.isSubmitting}>
            {proposalForm.formState.isSubmitting ? "Creating…" : "Add proposal"}
          </button>
        </form>
      ) : null}

      {proposals.isLoading && selectedClientId ? <LoadingState label="Loading proposals" /> : null}

      {proposals.data?.length ? (
        <section>
          <h2>Proposals</h2>
          <ul className="resource-list">
            {proposals.data.map((proposal) => (
              <li key={proposal.id}>
                <strong>{proposal.title}</strong>
                <p className="muted">
                  Status: {proposal.status} · Total: {proposal.currency} {proposal.totalAmount}
                </p>
                <button
                  type="button"
                  onClick={() => setSelectedProposalId(proposal.id)}
                >
                  {selectedProposalId === proposal.id ? "Selected" : "Manage"}
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : selectedClientId ? (
        <p className="muted">No proposals yet.</p>
      ) : null}

      {selectedProposal ? (
        <section>
          <h2>Proposal details</h2>
          <p className="muted">
            {selectedProposal.title} — {selectedProposal.status} · Subtotal {selectedProposal.currency}{" "}
            {selectedProposal.subtotal} · Total {selectedProposal.currency} {selectedProposal.totalAmount}
          </p>

          {selectedProposal.items.length ? (
            <ul className="resource-list">
              {selectedProposal.items.map((item) => (
                <li key={item.id}>
                  {item.description} — qty {item.quantity} × {item.unitPrice} = {item.lineTotal}
                </li>
              ))}
            </ul>
          ) : (
            <p className="muted">No line items yet.</p>
          )}

          {selectedProposal.status === "DRAFT" ? (
            <>
              <form
                onSubmit={itemForm.handleSubmit(async (values) => {
                  setError(null);
                  try {
                    await addProposalItem(selectedProposal.id, values);
                    itemForm.reset({ description: "", quantity: 1, unitPrice: 0, sortOrder: 0 });
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                })}
              >
                <h3>Add line item</h3>
                <label>
                  Description
                  <input {...itemForm.register("description")} />
                </label>
                <label>
                  Quantity
                  <input type="number" step="0.01" {...itemForm.register("quantity")} />
                </label>
                <label>
                  Unit price
                  <input type="number" step="0.01" {...itemForm.register("unitPrice")} />
                </label>
                <button type="submit" disabled={itemForm.formState.isSubmitting}>
                  {itemForm.formState.isSubmitting ? "Adding…" : "Add item"}
                </button>
              </form>

              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await sendProposal(selectedProposal.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Send proposal
              </button>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await cancelProposal(selectedProposal.id);
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

          {selectedProposal.status === "SENT" ? (
            <>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await approveProposal(selectedProposal.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Approve
              </button>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await rejectProposal(selectedProposal.id);
                    await refreshAll();
                  } catch (err) {
                    setError(err);
                  }
                }}
              >
                Reject
              </button>
              <button
                type="button"
                onClick={async () => {
                  setError(null);
                  try {
                    await cancelProposal(selectedProposal.id);
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
        </section>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
      {clients.error ? <ErrorState error={clients.error} /> : null}
      {proposals.error ? <ErrorState error={proposals.error} /> : null}
      {proposalDetail.error ? <ErrorState error={proposalDetail.error} /> : null}
    </section>
  );
}
