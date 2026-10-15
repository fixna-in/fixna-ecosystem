import { defineConfig } from "vitest/config";
import { fileURLToPath } from "node:url";

/**
 * Vitest configuration.
 *
 * Resolves the `@/*` path alias (mirroring tsconfig.json) so unit tests can
 * import app modules the same way the app does. Prior to this, `vitest run`
 * could not resolve `@/lib/...` imports and the api-schemas suite failed to
 * load — this makes the existing tests runnable.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
  },
});
