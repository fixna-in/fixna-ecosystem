import type { Metadata, Viewport } from "next";
import { AppShell } from "@/components/app-shell";
import { ThemeProvider } from "@fixna/design-system";
import { themeInitScript } from "@fixna/design-system/theme";
import "@fixna/design-system/tokens.css";
import "@fixna/design-system/base.css";
import "./globals.css";
import "./product.css";
import "./workspace.css";
import "./responsive.css";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "Fixna LocalBoost",
  description:
    "AI-assisted local advertising orchestration for SMBs — plan, launch and measure hyperlocal campaigns.",
  applicationName: "Fixna LocalBoost",
};

export const viewport: Viewport = {
  themeColor: "#163e32",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
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


