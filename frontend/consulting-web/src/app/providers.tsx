"use client";

import { useState } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AuthProvider } from "@/lib/auth-context";
import { toApiError } from "@/lib/api-client";

export function LoadingState({ label }: { label: string }) {
  return <p role="status">{label}…</p>;
}

export function ErrorState({ error }: { error: unknown }) {
  const apiError = toApiError(error);
  return (
    <div role="alert">
      <p>{apiError.code}: {apiError.message}</p>
    </div>
  );
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: false },
        },
      }),
  );
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}
