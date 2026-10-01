"use client";

import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { listClients } from "@/lib/clients-api";
import {
  createEngagement,
  createProject,
  createTask,
  EngagementInputSchema,
  listEngagements,
  listProjects,
  listTasks,
  ProjectInputSchema,
  TaskInputSchema,
  transitionEngagement,
  transitionProject,
  transitionTask,
  type EngagementInput,
  type ProjectInput,
  type TaskInput,
} from "@/lib/projects-api";
import { ErrorState, LoadingState } from "../providers";

export default function ProjectsPage() {
  const [error, setError] = useState<unknown>(null);
  const [selectedClientId, setSelectedClientId] = useState("");
  const [selectedEngagementId, setSelectedEngagementId] = useState("");
  const [selectedProjectId, setSelectedProjectId] = useState("");

  const clients = useQuery({ queryKey: ["clients"], queryFn: listClients, retry: false });
  const engagements = useQuery({
    queryKey: ["engagements", selectedClientId],
    queryFn: () => listEngagements(selectedClientId),
    enabled: Boolean(selectedClientId),
    retry: false,
  });
  const projects = useQuery({ queryKey: ["projects"], queryFn: listProjects, retry: false });
  const tasks = useQuery({
    queryKey: ["tasks", selectedProjectId],
    queryFn: () => listTasks(selectedProjectId),
    enabled: Boolean(selectedProjectId),
    retry: false,
  });

  const engagementForm = useForm<EngagementInput>({
    resolver: zodResolver(EngagementInputSchema),
  });
  const projectForm = useForm<ProjectInput>({
    resolver: zodResolver(ProjectInputSchema),
    defaultValues: { engagementId: "" },
  });
  const taskForm = useForm<TaskInput>({ resolver: zodResolver(TaskInputSchema) });

  async function refreshAll() {
    await Promise.all([
      clients.refetch(),
      engagements.refetch(),
      projects.refetch(),
      tasks.refetch(),
    ]);
  }

  return (
    <section aria-label="Projects">
      <h1>Projects</h1>
      <p className="muted">Create engagements, projects, and tasks; manage lifecycle status.</p>

      <label>
        Client (for engagements)
        <select
          value={selectedClientId}
          onChange={(event) => {
            setSelectedClientId(event.target.value);
            setSelectedEngagementId("");
            projectForm.setValue("engagementId", "");
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
          onSubmit={engagementForm.handleSubmit(async (values) => {
            setError(null);
            try {
              const engagement = await createEngagement(selectedClientId, values);
              engagementForm.reset();
              setSelectedEngagementId(engagement.id);
              projectForm.setValue("engagementId", engagement.id);
              await refreshAll();
            } catch (err) {
              setError(err);
            }
          })}
        >
          <h2>Create engagement</h2>
          <label>
            Title
            <input {...engagementForm.register("title")} />
          </label>
          {engagementForm.formState.errors.title ? (
            <p role="alert">{engagementForm.formState.errors.title.message}</p>
          ) : null}
          <button type="submit" disabled={engagementForm.formState.isSubmitting}>
            {engagementForm.formState.isSubmitting ? "Creating…" : "Add engagement"}
          </button>
        </form>
      ) : null}

      {engagements.data?.length ? (
        <section>
          <h2>Engagements</h2>
          <ul className="resource-list">
            {engagements.data.map((engagement) => (
              <li key={engagement.id}>
                <strong>{engagement.title}</strong>
                <p className="muted">Status: {engagement.status}</p>
                {engagement.status === "DRAFT" ? (
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await transitionEngagement(engagement.id, "ACTIVE");
                        setSelectedEngagementId(engagement.id);
                        projectForm.setValue("engagementId", engagement.id);
                        await refreshAll();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Activate
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <form
        onSubmit={projectForm.handleSubmit(async (values) => {
          setError(null);
          try {
            const project = await createProject({
              ...values,
              engagementId: values.engagementId || selectedEngagementId,
            });
            projectForm.reset({ engagementId: selectedEngagementId, name: "" });
            setSelectedProjectId(project.id);
            await refreshAll();
          } catch (err) {
            setError(err);
          }
        })}
      >
        <h2>Create project</h2>
        <label>
          Engagement
          <select {...projectForm.register("engagementId")}>
            <option value="">Select engagement</option>
            {engagements.data?.map((engagement) => (
              <option key={engagement.id} value={engagement.id}>
                {engagement.title}
              </option>
            ))}
          </select>
        </label>
        <label>
          Name
          <input {...projectForm.register("name")} />
        </label>
        {projectForm.formState.errors.name ? (
          <p role="alert">{projectForm.formState.errors.name.message}</p>
        ) : null}
        <button type="submit" disabled={projectForm.formState.isSubmitting}>
          {projectForm.formState.isSubmitting ? "Creating…" : "Add project"}
        </button>
      </form>

      {projects.isLoading ? (
        <LoadingState label="Loading projects" />
      ) : projects.data?.length ? (
        <section>
          <h2>Projects</h2>
          <ul className="resource-list">
            {projects.data.map((project) => (
              <li key={project.id}>
                <strong>{project.name}</strong>
                <p className="muted">Status: {project.status}</p>
                <button
                  type="button"
                  onClick={() => setSelectedProjectId(project.id)}
                >
                  {selectedProjectId === project.id ? "Selected" : "Select for tasks"}
                </button>
                {project.status === "PLANNED" ? (
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await transitionProject(project.id, "IN_PROGRESS");
                        await refreshAll();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Start
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : (
        <p className="muted">No projects yet.</p>
      )}

      {selectedProjectId ? (
        <form
          onSubmit={taskForm.handleSubmit(async (values) => {
            setError(null);
            try {
              await createTask(selectedProjectId, values);
              taskForm.reset();
              await refreshAll();
            } catch (err) {
              setError(err);
            }
          })}
        >
          <h2>Create task</h2>
          <label>
            Title
            <input {...taskForm.register("title")} />
          </label>
          {taskForm.formState.errors.title ? (
            <p role="alert">{taskForm.formState.errors.title.message}</p>
          ) : null}
          <button type="submit" disabled={taskForm.formState.isSubmitting}>
            {taskForm.formState.isSubmitting ? "Creating…" : "Add task"}
          </button>
        </form>
      ) : null}

      {tasks.data?.length ? (
        <section>
          <h2>Tasks</h2>
          <ul className="resource-list">
            {tasks.data.map((task) => (
              <li key={task.id}>
                <strong>{task.title}</strong>
                <p className="muted">Status: {task.status}</p>
                {task.status === "TODO" ? (
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await transitionTask(task.id, "IN_PROGRESS");
                        await refreshAll();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Start task
                  </button>
                ) : null}
                {task.status === "IN_PROGRESS" ? (
                  <button
                    type="button"
                    onClick={async () => {
                      setError(null);
                      try {
                        await transitionTask(task.id, "DONE");
                        await refreshAll();
                      } catch (err) {
                        setError(err);
                      }
                    }}
                  >
                    Complete
                  </button>
                ) : null}
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {error ? <ErrorState error={error} /> : null}
      {clients.error ? <ErrorState error={clients.error} /> : null}
      {projects.error ? <ErrorState error={projects.error} /> : null}
    </section>
  );
}
