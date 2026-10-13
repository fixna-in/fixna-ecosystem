import type { Metadata } from "next";
import { AppShell } from "@/components/app-shell";
import { Providers } from "./providers";
import { ThemeProvider } from "@fixna/design-system";
import { themeInitScript } from "@fixna/design-system/theme";
import "@fixna/design-system/tokens.css";
import "@fixna/design-system/base.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Fixna Consulting",
    template: "%s | Fixna Consulting",
  },
  description: "Client relationship management for consulting firms.",
  applicationName: "Fixna Consulting",
  icons: {
    icon: "/icon.svg",
    apple: "/apple-icon.svg",
  },
  manifest: "/manifest.webmanifest",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        {/* Apply the persisted/system theme before paint to avoid a flash. */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <ThemeProvider>
          <Providers>
            <AppShell>{children}</AppShell>
          </Providers>
        </ThemeProvider>
      </body>
    </html>
  );
}
