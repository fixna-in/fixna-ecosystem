'use client';

import { useState } from 'react';
import Link from 'next/link';
import { ThemeToggle } from '@fixna/design-system';

export default function Home() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);

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
              {isAuthenticated ? (
                <>
                  <Link href="/dashboard" className="text-muted-foreground hover:text-foreground">
                    Dashboard
                  </Link>
                  <button
                    onClick={() => setIsAuthenticated(false)}
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

      <div className="flex-1 flex items-center justify-center px-4">
        <div className="max-w-3xl text-center">
          <h1 className="text-4xl sm:text-5xl font-bold tracking-tight text-foreground mb-6">
            Hospitality Management Made Simple
          </h1>
          <p className="text-lg text-muted-foreground mb-8 max-w-2xl mx-auto">
            Streamline your hotel, restaurant, or venue operations with Fixna's
            purpose-built platform. Manage bookings, staff, inventory, and guest
            experiences all in one place.
          </p>
          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
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

      <footer className="border-t border-border bg-surface py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center text-muted-foreground">
          <p>&copy; {new Date().getFullYear()} Fixna. All rights reserved.</p>
        </div>
      </footer>
    </main>
  );
}
