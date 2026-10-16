'use client';

import { ReactNode } from 'react';
import { AuthProvider } from '@/lib/auth-context';

/**
 * Client-side providers for hospitality-web. Must be a separate client
 * component so the server layout can stay a Server Component.
 */
export function Providers({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}
