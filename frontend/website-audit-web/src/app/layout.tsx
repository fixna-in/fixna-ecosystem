import type { Metadata } from 'next';
import { Inter, IBM_Plex_Mono, Instrument_Serif } from 'next/font/google';
import { ThemeProvider } from '@fixna/design-system';
import { themeInitScript } from '@fixna/design-system/theme';
import './globals.css';

const inter = Inter({ subsets: ['latin'], variable: '--font-inter', display: 'swap' });
const ibmPlexMono = IBM_Plex_Mono({ subsets: ['latin'], weight: ['400', '500', '600', '700'], variable: '--font-ibm-plex-mono', display: 'swap' });
const instrumentSerif = Instrument_Serif({ subsets: ['latin'], weight: ['400'], style: ['normal', 'italic'], variable: '--font-instrument-serif', display: 'swap' });

export const metadata: Metadata = {
  title: 'Fixna Website Audit',
  description: 'Deterministic website audits with advisory AI fixes',
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
      <body className="antialiased">
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}

