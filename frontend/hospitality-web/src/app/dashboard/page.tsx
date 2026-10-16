'use client';

import { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { ThemeToggle } from '@fixna/design-system';

export default function DashboardPage() {
  const router = useRouter();
  const { user, isLoading, logout } = useAuth();

  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/login');
    }
  }, [isLoading, user, router]);

  const handleSignOut = async () => {
    await logout();
    router.replace('/');
  };

  const displayName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
      : user?.email;

  return (
    <main className="min-h-screen flex flex-col">
      <header className="border-b border-border bg-surface">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16 gap-3">
            <Link href="/" className="text-xl font-bold text-foreground">
              Fixna Hospitality
            </Link>
            <nav className="flex items-center gap-3">
              <span className="text-muted-foreground hidden sm:inline">{user?.email}</span>
              <button
                onClick={handleSignOut}
                className="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-lg hover:bg-primary-hover transition-colors"
              >
                Sign out
              </button>
              <ThemeToggle className="theme-toggle" label="Toggle color theme" />
            </nav>
          </div>
        </div>
      </header>

      <div className="flex-1 px-4 py-12">
        <div className="max-w-5xl mx-auto">
          {isLoading || !user ? (
            <p className="text-muted-foreground" role="status">
              Loading your workspace…
            </p>
          ) : (
            <>
              <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-primary mb-3">
                Hospitality operations
              </p>
              <h1 className="text-3xl font-bold tracking-tight text-foreground mb-2">
                Welcome{displayName ? `, ${displayName}` : ''}
              </h1>
              <p className="text-muted-foreground mb-8">
                You are signed in to your Fixna Hospitality workspace.
              </p>

              <div className="grid gap-4 sm:grid-cols-3">
                <article className="bg-surface border border-border rounded-xl p-5">
                  <h2 className="font-semibold text-foreground mb-2">Bookings</h2>
                  <p className="text-sm text-muted-foreground">
                    Track arrivals, departures, and availability from the daily
                    overview. Coming soon.
                  </p>
                </article>
                <article className="bg-surface border border-border rounded-xl p-5">
                  <h2 className="font-semibold text-foreground mb-2">Service</h2>
                  <p className="text-sm text-muted-foreground">
                    Guest requests and staff ownership during shift changes.
                    Coming soon.
                  </p>
                </article>
                <article className="bg-surface border border-border rounded-xl p-5">
                  <h2 className="font-semibold text-foreground mb-2">Operations</h2>
                  <p className="text-sm text-muted-foreground">
                    Inventory, schedules, and daily summaries in one workspace.
                    Coming soon.
                  </p>
                </article>
              </div>
            </>
          )}
        </div>
      </div>
    </main>
  );
}
