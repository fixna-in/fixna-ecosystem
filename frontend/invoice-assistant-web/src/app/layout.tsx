import type { Metadata } from 'next';
import { ThemeProvider } from '@fixna/design-system';
import { themeInitScript } from '@fixna/design-system/theme';
import './globals.css';

export const metadata: Metadata = {
  title: 'Fixna Invoice Assistant',
  description: 'Turn invoices and receipts into reviewable, exportable records',
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
      <body className="antialiased">
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}

