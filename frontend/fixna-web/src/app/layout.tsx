import type { Metadata } from "next";
import { PageShell } from "@/components/page-shell";
import "./globals.css";

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_SITE_URL || "http://localhost:3002"),
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
    card: "summary_large_image",
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

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <PageShell>{children}</PageShell>
      </body>
    </html>
  );
}
