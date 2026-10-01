"use client";

import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useState } from "react";
import { LoginSchema, type LoginInput } from "@/lib/auth-api";
import { useAuth } from "@/lib/auth-context";
import { ErrorState } from "../providers";

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [error, setError] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(LoginSchema) });

  return (
    <section>
      <h1>Sign in</h1>
      <p className="muted">Access your consulting workspace.</p>
      <form
        onSubmit={handleSubmit(async (values) => {
          setError(null);
          try {
            await login(values);
            router.push("/clients");
          } catch (err) {
            setError(err);
          }
        })}
      >
        <label>
          Email
          <input type="email" autoComplete="email" {...register("email")} />
        </label>
        {errors.email ? <p role="alert">{errors.email.message}</p> : null}
        <label>
          Password
          <input type="password" autoComplete="current-password" {...register("password")} />
        </label>
        {errors.password ? <p role="alert">{errors.password.message}</p> : null}
        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Signing in…" : "Sign in"}
        </button>
      </form>
      {error ? <ErrorState error={error} /> : null}
      <p>
        New here? <Link href="/register">Create an account</Link>
      </p>
    </section>
  );
}
