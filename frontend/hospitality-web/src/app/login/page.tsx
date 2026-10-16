'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/lib/auth-context';
import { ThemeToggle } from '@fixna/design-system';

interface LoginValues {
  email: string;
  password: string;
}

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>();

  return (
    <main className="min-h-screen flex flex-col">
      <header className="border-b border-border bg-surface">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 gap-3">
            <Link href="/" className="text-xl font-bold text-foreground">
              Fixna Hospitality
            </Link>
            <nav className="flex items-center gap-3">
              <Link href="/register" className="text-muted-foreground hover:text-foreground">
                Create account
              </Link>
              <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            </nav>
          </div>
        </div>
      </header>

      <div className="flex-1 flex items-center justify-center px-4 py-12">
        <section className="w-full max-w-md bg-surface border border-border rounded-2xl p-8" aria-labelledby="login-heading">
          <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-primary mb-3">
            Hospitality operations
          </p>
          <h1 id="login-heading" className="text-2xl font-bold text-foreground mb-2">
            Welcome back
          </h1>
          <p className="text-muted-foreground mb-6">
            Sign in to your Fixna Hospitality workspace.
          </p>

          <form
            className="space-y-4"
            onSubmit={handleSubmit(async (values) => {
              setServerError(null);
              try {
                await login(values.email, values.password);
                router.push('/dashboard');
              } catch {
                setServerError('Invalid email or password. Please try again.');
              }
            })}
          >
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-foreground mb-1">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                className="w-full px-3 py-2 rounded-lg bg-input border border-border text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('email', {
                  required: 'Email is required',
                  pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email address' },
                })}
              />
              {errors.email ? (
                <p role="alert" className="mt-1 text-sm text-danger">{errors.email.message}</p>
              ) : null}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-foreground mb-1">
                Password
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                className="w-full px-3 py-2 rounded-lg bg-input border border-border text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('password', {
                  required: 'Password is required',
                  minLength: { value: 8, message: 'Password must be at least 8 characters' },
                })}
              />
              {errors.password ? (
                <p role="alert" className="mt-1 text-sm text-danger">{errors.password.message}</p>
              ) : null}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full px-4 py-2.5 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover disabled:opacity-60 transition-colors"
            >
              {isSubmitting ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          {serverError ? (
            <p role="alert" className="mt-4 text-sm text-danger bg-danger-soft border border-border rounded-lg px-3 py-2">
              {serverError}
            </p>
          ) : null}

          <p className="mt-6 text-sm text-muted-foreground">
            New to Fixna?{' '}
            <Link href="/register" className="text-primary hover:underline">
              Create an account
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}
