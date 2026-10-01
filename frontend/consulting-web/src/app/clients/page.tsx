"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import {
  ClientInputSchema,
  createClient,
  listClients,
  type ClientInput,
} from "@/lib/clients-api";
import { ErrorState, LoadingState } from "../providers";

export default function ClientsPage() {
  const [error, setError] = useState<unknown>(null);
  const clients = useQuery({
    queryKey: ["clients"],
    queryFn: listClients,
    retry: false,
  });
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ClientInput>({ resolver: zodResolver(ClientInputSchema) });

  return (
    <section aria-label="Clients">
      <h1>Clients</h1>
      <p className="muted">Manage your consulting client relationships.</p>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await createClient(values);
            reset();
            clients.refetch();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <h2>Add client</h2>
        <label>
          Name
          <input {...register("name")} />
        </label>
        {errors.name ? <p role="alert">{errors.name.message}</p> : null}
        <label>
          Email
          <input type="email" {...register("email")} />
        </label>
        <label>
          Industry
          <input {...register("industry")} />
        </label>
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Add client"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      {clients.isLoading ? (
        <LoadingState label="Loading clients" />
      ) : clients.error ? (
        <ErrorState error={clients.error} />
      ) : clients.data?.length ? (
        <ul className="resource-list">
          {clients.data.map((client) => (
            <li key={client.id}>
              <strong>{client.name}</strong>
              {client.industry ? <p className="muted">{client.industry}</p> : null}
              {client.email ? <p>{client.email}</p> : null}
            </li>
          ))}
        </ul>
      ) : (
        <p className="muted">No clients yet. Add your first client above.</p>
      )}
    </section>
  );
}
