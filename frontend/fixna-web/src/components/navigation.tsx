"use client";

import Link from "next/link";
import { useState } from "react";
import { navigation } from "@/content/navigation";
import { productLinks } from "@/lib/product-links";
import { FixnaBrandMark } from "@/components/brand-mark";

export function Header() {
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link className="brand" href="/" aria-label="Fixna home" onClick={() => setMobileOpen(false)}>
          <FixnaBrandMark />
          <span className="brand-wordmark" aria-hidden="true">
            <span>Fixna</span>
          </span>
        </Link>

        <nav className="main-nav" aria-label="Primary navigation">
          {navigation.map((item) => (
            <Link key={item.href} href={item.href}>{item.label}</Link>
          ))}
          <Link className="button button-primary nav-cta" href="/contact">Contact</Link>
        </nav>

        <button
          type="button"
          className="mobile-menu-button"
          aria-label={mobileOpen ? "Close menu" : "Open menu"}
          aria-expanded={mobileOpen}
          onClick={() => setMobileOpen((open) => !open)}
        >
          <span />
          <span />
          <span />
        </button>
      </div>

      {mobileOpen && (
        <nav className="mobile-nav" aria-label="Mobile navigation">
          {navigation.map((item) => (
            <Link key={item.href} href={item.href} onClick={() => setMobileOpen(false)}>
              {item.label}
            </Link>
          ))}
          <Link className="button button-primary mobile-cta" href="/contact" onClick={() => setMobileOpen(false)}>
            Contact Us
          </Link>
          <div className="mobile-products">
            <p>Products</p>
            <Link href={productLinks.localboost} target="_blank" rel="noreferrer" onClick={() => setMobileOpen(false)}>
              LocalBoost <span aria-hidden="true">↗</span>
            </Link>
            <Link href={productLinks.consulting} target="_blank" rel="noreferrer" onClick={() => setMobileOpen(false)}>
              Consulting <span aria-hidden="true">↗</span>
            </Link>
            <Link href="/products" onClick={() => setMobileOpen(false)}>
              Hospitality <span className="status">Coming Soon</span>
            </Link>
          </div>
        </nav>
      )}
    </header>
  );
}
