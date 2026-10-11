import {
  createContactHandler,
  createInMemoryRateLimiter,
} from "./handler";
import { appendRowToSheet, isSheetsConfigured } from "./sheets";

const RATE_LIMIT = Number(process.env.CONTACT_RATE_LIMIT_PER_IP ?? 3);
const RATE_WINDOW_MS = Number(process.env.CONTACT_RATE_WINDOW_MS ?? 10 * 60 * 1000);

const consumeRateLimit = createInMemoryRateLimiter(RATE_LIMIT, RATE_WINDOW_MS);

export const POST = createContactHandler({
  appendRow: appendRowToSheet,
  isConfigured: isSheetsConfigured,
  consumeRateLimit,
});
