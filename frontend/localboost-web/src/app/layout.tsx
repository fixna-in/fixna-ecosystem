import type { Metadata, Viewport } from "next";
import { Inter, IBM_Plex_Mono, Instrument_Serif } from "next/font/google";
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

const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });
const ibmPlexMono = IBM_Plex_Mono({ subsets: ["latin"], weight: ["400", "500", "600", "700"], variable: "--font-ibm-plex-mono", display: "swap" });
const instrumentSerif = Instrument_Serif({ subsets: ["latin"], weight: ["400"], style: ["normal", "italic"], variable: "--font-instrument-serif", display: "swap" });

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
    <html lang="en" className={`${inter.variable} ${ibmPlexMono.variable} ${instrumentSerif.variable}`} suppressHydrationWarning>
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


