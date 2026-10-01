"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { listClients } from "@/lib/clients-api";
import {
  createMeeting,
  listMeetings,
  MeetingInputSchema,
  transitionMeeting,
  type MeetingInput,
} from "@/lib/meetings-api";
import { ErrorState, LoadingState } from "../providers";

function toIsoFromLocalInput(value: string): string {
  return new Date(value).toISOString();
}

export default function MeetingsPage() {
  const [error, setError] = useState<unknown>(null);
  const [selectedClientId, setSelectedClientId] = useState("");

  const clients = useQuery({ queryKey: ["clients"], queryFn: listClients, retry: false });
  const meetings = useQuery({
    queryKey: ["meetings", selectedClientId],
    queryFn: () => listMeetings(selectedClientId ? { clientId: selectedClientId } : undefined),
    retry: false,
  });

  const form = useForm<MeetingInput>({
    resolver: zodResolver(MeetingInputSchema),
    defaultValues: {
      clientId: "",
      title: "",
      startsAt: "",
      endsAt: "",
    },
  });

  async function refreshAll() {
    await Promise.all([clients.refetch(), meetings.refetch()]);
  }

  return (
    <section aria-label="Meetings">
      <h1>Meetings</h1>
      <p className="muted">Schedule meetings and manage lifecycle status.</p>

      <label>
        Filter by client
        <select
          value={selectedClientId}
          onChange={(event) => {
            setSelectedClientId(event.target.value);
            form.setValue("clientId", event.target.value);
          }}
        >
          <option value="">All clients</option>
          {clients.data?.map((client) => (
            <option key={client.id} value={client.id}>
              {client.name}
            </option>
          ))}
        </select>
      </label>

      <form
        onSubmit={form.handleSubmit(async (values) => {
          setError(null);
          try {
            await createMeeting({
              ...values,
              startsAt: toIsoFromLocalInput(values.startsAt),
              endsAt: toIsoFromLocalInput(values.endsAt),
            });
            form.reset({
              clientId: selectedClientId,
              title: "",
              startsAt: "",
              endsAt: "",
            });
            await refreshAll();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <h2>Schedule meeting</h2>
        <label>
          Client
          <select {...form.register("clientId")}>
            <option value="">Select a client</option>
            {clients.data?.map((client) => (
              <option key={client.id} value={client.id}>
                {client.name}
              </option>
            ))}
          </select>
        </label>
        {form.formState.errors.clientId ? (
          <p role="alert">{form.formState.errors.clientId.message}</p>
        ) : null}
        <label>
          Title
          <input {...form.register("title")} />
        </label>
        {form.formState.errors.title ? (
          <p role="alert">{form.formState.errors.title.message}</p>
        ) : null}
        <label>
          Location
          <input {...form.register("location")} />
        </label>
        <label>
          Starts
          <input type="datetime-local" {...form.register("startsAt")} />
        </label>
        <label>
          Ends
          <input type="datetime-local" {...form.register("endsAt")} />
        </label>
        <button type="submit" disabled={form.formState.isSubmitting}>
          {form.formState.isSubmitting ? "Scheduling…" : "Schedule meeting"}
        </button>
      </form>

      {meetings.isLoading ? (
        <LoadingState label="Loading meetings" />
      ) : meetings.data?.length ? (
        <section>
          <h2>Meetings</h2>
          <ul className="resource-list">
            {meetings.data.map((meeting) => (
              <li key={meeting.id}>
                <strong>{meeting.title}</strong>
                <p className="muted">
                  {new Date(meeting.startsAt).toLocaleString()} –{" "}
                  {new Date(meeting.endsAt).toLocaleString()}
                  {meeting.location ? ` · ${meeting.location}` : ""}
                </p>
                <p className="muted">Status: {meeting.status}</p>
                {meeting.status === "SCHEDULED" ? (
                  <>
                    <button
                      type="button"
                      onClick={async () => {
                        setError(null);
                        try {
                          await transitionMeeting(meeting.id, "IN_PROGRESS");
                          await refreshAll();
                        } catch (err) {
                          setError(err);
                        }
                      }}
                    >
                      Start
                    </button>
                    <button
                      type="button"
                      onClick={async () => {
                        setError(null);
                        try {
                          await transitionMeeting(meeting.id, "CANCELLED");
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
                {meeting.status === "IN_PROGRESS" ? (
                  <>
                    <button
                      type="button"
                      onClick={async () => {
                        setError(null);
                        try {
                          await transitionMeeting(meeting.id, "COMPLETED");
                          await refreshAll();
                        } catch (err) {
                          setError(err);
                        }
                      }}
                    >
                      Complete
                    </button>
                    <button
                      type="button"
                      onClick={async () => {
                        setError(null);
                        try {
                          await transitionMeeting(meeting.id, "CANCELLED");
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
              </li>
            ))}
          </ul>
        </section>
      ) : (
        <p className="muted">No meetings yet.</p>
      )}

      {error ? <ErrorState error={error} /> : null}
      {clients.error ? <ErrorState error={clients.error} /> : null}
      {meetings.error ? <ErrorState error={meetings.error} /> : null}
    </section>
  );
}
