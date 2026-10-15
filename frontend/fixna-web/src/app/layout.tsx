import type { Metadata } from "next";
import { Inter, IBM_Plex_Mono, Instrument_Serif } from "next/font/google";
import { PageShell } from "@/components/page-shell";
import { siteUrl } from "@/lib/site-url";
import { ThemeProvider } from "@fixna/design-system";
import { themeInitScript } from "@fixna/design-system/theme";
import "@fixna/design-system/tokens.css";
import "./design-system.css";
import "./globals.css";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });
const ibmPlexMono = IBM_Plex_Mono({ subsets: ["latin"], weight: ["400", "500", "600", "700"], variable: "--font-ibm-plex-mono", display: "swap" });
const instrumentSerif = Instrument_Serif({ subsets: ["latin"], weight: ["400"], style: ["normal", "italic"], variable: "--font-instrument-serif", display: "swap" });

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    default: "Fixna — Technology, Product Engineering & AI Solutions",
    template: "%s | Fixna",
  },
  description:
    "Fixna builds practical software, cloud-native platforms, event-driven systems, AI-enabled products, automation, and technology consulting for real business problems.",
  applicationName: "Fixna",
  keywords: ["software engineering", "solution architecture", "cloud", "Confluent Kafka", "AI", "GenAI", "automation", "technology consulting"],
  authors: [{ name: "Fixna" }],
  creator: "Fixna",
  openGraph: {
    title: "Fixna — Technology, Product Engineering & AI Solutions",
    description:
      "We build practical software for real business problems through product engineering, cloud architecture, event-driven systems, AI, and automation.",
    url: "/",
    siteName: "Fixna",
    locale: "en_US",
    type: "website",
  },
  twitter: {
    // No social preview image exists yet — use the text-only summary card
    // rather than claiming a large image (R3).
    card: "summary",
    title: "Fixna — Technology, Product Engineering & AI Solutions",
    description:
      "We build practical software for real business problems through product engineering, cloud architecture, event-driven systems, AI, and automation.",
  },
  icons: {
    icon: "/icon.svg",
    apple: "/apple-icon.svg",
  },
  robots: {
    index: true,
    follow: true,
  },
};

const structuredData = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "Organization",
      "@id": `${siteUrl}/#organization`,
      name: "Fixna",
      url: siteUrl,
      description:
        "Technology, product engineering and AI solutions company.",
    },
    {
      "@type": "WebSite",
      "@id": `${siteUrl}/#website`,
      url: siteUrl,
      name: "Fixna",
      publisher: { "@id": `${siteUrl}/#organization` },
    },
  ],
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${inter.variable} ${ibmPlexMono.variable} ${instrumentSerif.variable}`} suppressHydrationWarning>
      <head>
        {/* Apply the persisted/system theme before paint to avoid a flash. */}
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(structuredData) }}
        />
        <ThemeProvider>
          <PageShell>{children}</PageShell>
        </ThemeProvider>
      </body>
    </html>
  );
}
