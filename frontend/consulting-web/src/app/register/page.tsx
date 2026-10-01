"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { RegisterSchema, type RegisterInput } from "@/lib/auth-api";
import { useAuth } from "@/lib/auth-context";
import { ErrorState } from "../providers";

export default function RegisterPage() {
  const router = useRouter();
  const { register: registerUser } = useAuth();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterInput>({ resolver: zodResolver(RegisterSchema) });

  return (
    <section>
      <h1>Create workspace</h1>
      <p className="muted">Register your consulting firm on Fixna.</p>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await registerUser(values);
            router.push("/clients");
          } catch (err) {
            setError(err);
          }
        })}
      >
        <label>
          Workspace name
          <input {...register("tenantName")} />
        </label>
        {errors.tenantName ? <p role="alert">{errors.tenantName.message}</p> : null}
        <label>
          Email
          <input type="email" autoComplete="email" {...register("email")} />
        </label>
        {errors.email ? <p role="alert">{errors.email.message}</p> : null}
        <label>
          Password
          <input type="password" autoComplete="new-password" {...register("password")} />
        </label>
        {errors.password ? <p role="alert">{errors.password.message}</p> : null}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Creating…" : "Create workspace"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      <p>
        Already registered? <Link href="/login">Sign in</Link>
      </p>
    </section>
  );
}
