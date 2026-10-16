'use client';

import Link from 'next/link';
import { useAuth } from '@/lib/auth-context';
import { ThemeToggle } from '@fixna/design-system';

export default function Home() {
  const { user, logout } = useAuth();

  return (
    <main className="min-h-screen flex flex-col">
      <header className="border-b border-border bg-surface">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 gap-3">
            <div className="flex items-center">
              <Link href="/" className="text-xl font-bold text-foreground">
                Fixna Hospitality
              </Link>
            </div>
            <nav className="flex items-center gap-3">
              {user ? (
                <>
                  <Link href="/dashboard" className="text-muted-foreground hover:text-foreground">
                    Dashboard
                  </Link>
                  <button
                    onClick={() => {
                      void logout();
                    }}
                    className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover"
                  >
                    Sign out
                  </button>
                </>
              ) : (
                <>
                  <Link href="/login" className="text-muted-foreground hover:text-foreground">
                    Sign in
                  </Link>
                  <Link
                    href="/register"
                    className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover"
                  >
                    Get started
                  </Link>
                </>
              )}
              <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            </nav>
          </div>
        </div>
      </header>

      <div className="flex-1 flex items-start px-4 py-16 sm:py-24">
        <div className="max-w-3xl">
          <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-primary mb-4">
            Hospitality operations
          </p>
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-foreground mb-6">
            Hospitality Management Made Simple
          </h1>
          <p className="text-lg text-muted-foreground mb-8 max-w-2xl">
            Streamline your hotel, restaurant, or venue operations with Fixna's
            purpose-built platform. Manage bookings, staff, inventory, and guest
            experiences all in one place.
          </p>
          <div className="flex flex-col sm:flex-row items-start gap-4">
            <Link
              href="/register"
              className="w-full sm:w-auto px-8 py-3 text-base font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover transition-colors"
            >
              Start Free Trial
            </Link>
            <Link
              href="/login"
              className="w-full sm:w-auto px-8 py-3 text-base font-medium text-foreground bg-surface-muted rounded-lg hover:opacity-80 transition-colors"
            >
              Sign In
            </Link>
          </div>
        </div>
      </div>

      <section className="border-t border-border bg-surface py-12">
        <div className="max-w-5xl mx-auto px-4">
          <h2 className="text-2xl font-semibold text-foreground mb-3">
            Built for front-desk and operations teams
          </h2>
          <p className="text-muted-foreground mb-6 max-w-3xl">
            Keep the workflows that matter most visible: guest status, bookings,
            staff coverage, inventory movement, and service follow-up. The
            interface is designed to stay calm under pressure and quick to scan
            in both light and dark themes.
          </p>
          <div className="grid gap-4 sm:grid-cols-3">
            <article className="bg-surface-muted border border-border rounded-xl p-5">
              <h3 className="font-semibold text-foreground mb-2">Bookings</h3>
              <p className="text-sm text-muted-foreground">
                Track arrivals, departures, and availability without leaving the
                daily overview.
              </p>
            </article>
            <article className="bg-surface-muted border border-border rounded-xl p-5">
              <h3 className="font-semibold text-foreground mb-2">Service</h3>
              <p className="text-sm text-muted-foreground">
                Surface guest requests and staff ownership so nothing is lost
                during shift changes.
              </p>
            </article>
            <article className="bg-surface-muted border border-border rounded-xl p-5">
              <h3 className="font-semibold text-foreground mb-2">Operations</h3>
              <p className="text-sm text-muted-foreground">
                Monitor inventory, schedules, and daily summaries from one
                consistent workspace.
              </p>
            </article>
          </div>
        </div>
      </section>

      <footer className="border-t border-border bg-surface py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center text-muted-foreground">
          <p>&copy; {new Date().getFullYear()} Fixna. All rights reserved.</p>
        </div>
      </footer>
    </main>
  );
}
