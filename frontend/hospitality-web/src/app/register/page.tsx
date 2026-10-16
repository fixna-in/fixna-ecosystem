'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { useAuth } from '@/lib/auth-context';
import { ThemeToggle } from '@fixna/design-system';

interface RegisterValues {
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
}

export default function RegisterPage() {
  const router = useRouter();
  const { register: signup } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterValues>();

  return (
    <main className="min-h-screen flex flex-col">
      <header className="border-b border-border bg-surface">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 gap-3">
            <Link href="/" className="text-xl font-bold text-foreground">
              Fixna Hospitality
            </Link>
            <nav className="flex items-center gap-3">
              <Link href="/login" className="text-muted-foreground hover:text-foreground">
                Sign in
              </Link>
              <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            </nav>
          </div>
        </div>
      </header>

      <div className="flex-1 flex items-center justify-center px-4 py-12">
        <section className="w-full max-w-md bg-surface border border-border rounded-2xl p-8" aria-labelledby="register-heading">
          <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-primary mb-3">
            Hospitality operations
          </p>
          <h1 id="register-heading" className="text-2xl font-bold text-foreground mb-2">
            Start your workspace
          </h1>
          <p className="text-muted-foreground mb-6">
            Create an account and your hospitality workspace in one step.
          </p>

          <form
            className="space-y-4"
            onSubmit={handleSubmit(async (values) => {
              setServerError(null);
              try {
                await signup({
                  email: values.email,
                  password: values.password,
                  firstName: values.firstName || undefined,
                  lastName: values.lastName || undefined,
                });
                router.push('/dashboard');
              } catch {
                setServerError('Could not create the account. The email may already be registered.');
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
                Password (min 8 characters)
              </label>
              <input
                id="password"
                type="password"
                autoComplete="new-password"
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
            <div>
              <label htmlFor="firstName" className="block text-sm font-medium text-foreground mb-1">
                First name (optional)
              </label>
              <input
                id="firstName"
                autoComplete="given-name"
                className="w-full px-3 py-2 rounded-lg bg-input border border-border text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('firstName', { maxLength: { value: 100, message: 'Too long' } })}
              />
              {errors.firstName ? (
                <p role="alert" className="mt-1 text-sm text-danger">{errors.firstName.message}</p>
              ) : null}
            </div>

            <div>
              <label htmlFor="lastName" className="block text-sm font-medium text-foreground mb-1">
                Last name (optional)
              </label>
              <input
                id="lastName"
                autoComplete="family-name"
                className="w-full px-3 py-2 rounded-lg bg-input border border-border text-foreground text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                {...register('lastName', { maxLength: { value: 100, message: 'Too long' } })}
              />
              {errors.lastName ? (
                <p role="alert" className="mt-1 text-sm text-danger">{errors.lastName.message}</p>
              ) : null}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full px-4 py-2.5 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover disabled:opacity-60 transition-colors"
            >
              {isSubmitting ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          {serverError ? (
            <p role="alert" className="mt-4 text-sm text-danger bg-danger-soft border border-border rounded-lg px-3 py-2">
              {serverError}
            </p>
          ) : null}

          <p className="mt-6 text-sm text-muted-foreground">
            Already have an account?{' '}
            <Link href="/login" className="text-primary hover:underline">
              Sign in
            </Link>
          </p>
        </section>
      </div>
    </main>
  );
}
