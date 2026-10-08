import type { ReactNode } from "react";
import { Footer } from "@/components/footer";
import { Header } from "@/components/navigation";

export function PageShell({ children }: { children: ReactNode }) {
  return (
    <div className="site-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>
      <Header />
      <main id="main-content">{children}</main>
      <Footer />
    </div>
  );
}
