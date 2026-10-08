"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { toApiError } from "@/lib/api-client";
import { useState } from "react";
import { BrandMarkIcon } from "@/brand/brand-mark-icon";

const navigation = [
  { href: "/clients", label: "Clients" },
  { href: "/projects", label: "Projects" },
  { href: "/meetings", label: "Meetings" },
  { href: "/proposals", label: "Proposals" },
  { href: "/invoices", label: "Invoices" },
  { href: "/website", label: "Website" },
  { href: "/ai", label: "AI Assistant" },
];

export function AppShell({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const { session, initialized, logout } = useAuth();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const publicPage =
    ["/", "/login", "/register", "/portal"].includes(pathname) || pathname.startsWith("/site/");

  if (publicPage) {
    return (
      <>
        <header className="public-header">
          <Link className="brand" href="/" aria-label="Fixna Consulting home">
            <BrandMarkIcon width={38} height={40} />
            <span className="brand-wordmark">fixna<span>Consulting</span></span>
          </Link>
          <nav aria-label="Public navigation">
            <Link href="/portal">Client portal</Link>
            <Link href="/login">Sign in</Link>
            <Link href="/register" className="header-cta">Create account</Link>
          </nav>
        </header>
        <main className="public-content">{children}</main>
        <footer className="public-footer">
          <span>© {new Date().getFullYear()} Fixna Consulting</span>
          <span>Built for ambitious advisory teams</span>
        </footer>
      </>
    );
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <Link className="brand" href="/" aria-label="Fixna Consulting home">
          <BrandMarkIcon width={38} height={40} />
          <span className="brand-wordmark">fixna<span>Consulting</span></span>
        </Link>
        <p className="workspace-label">Consultant workspace</p>
        <nav aria-label="Primary navigation">
          {navigation.map(({ href, label }) => {
            const active = pathname === href || pathname.startsWith(`${href}/`);
            return (
              <Link key={href} href={href} className={active ? "active" : undefined}>
                <span aria-hidden="true">•</span>
                {label}
              </Link>
            );
          })}
        </nav>
        <div className="sidebar-footer">
          {session ? (
            <button
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                setError(null);
                try {
                  await logout();
                  router.push("/login");
                } catch (err) {
                  setError(toApiError(err).message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? "Signing out…" : "Sign out"}
            </button>
          ) : (
            <Link href="/login" className="sidebar-login">Sign in</Link>
          )}
          {!session && initialized ? <p className="muted">Sign in to manage clients.</p> : null}
          {error ? <p className="sidebar-error" role="alert">{error}</p> : null}
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  );
}
