/**
 * Google Sheets adapter for the contact form.
 *
 * Uses the Google Sheets REST API with a service-account access token. The
 * service-account JSON is provided via the GOOGLE_SERVICE_ACCOUNT_JSON env
 * var (never committed, never exposed to the browser); the target sheet is
 * identified by GOOGLE_SHEET_ID and must be shared with the service
 * account's email as an editor.
 *
 * google-auth-library is imported lazily so unit tests never load it.
 */

const SHEET_RANGE = "Sheet1!A:F";

export function isSheetsConfigured(): boolean {
  return Boolean(process.env.GOOGLE_SERVICE_ACCOUNT_JSON && process.env.GOOGLE_SHEET_ID);
}

export async function appendRowToSheet(row: string[]): Promise<void> {
  const jsonKey = process.env.GOOGLE_SERVICE_ACCOUNT_JSON;
  const sheetId = process.env.GOOGLE_SHEET_ID;
  if (!jsonKey || !sheetId) {
    throw new Error("Contact form storage is not configured");
  }

  const { GoogleAuth } = await import("google-auth-library");
  const auth = new GoogleAuth({
    credentials: JSON.parse(jsonKey),
    scopes: ["https://www.googleapis.com/auth/spreadsheets"],
  });
  const client = await auth.getClient();
  const tokenResponse = await client.getAccessToken();
  const token = typeof tokenResponse === "string" ? tokenResponse : tokenResponse?.token;
  if (!token) {
    throw new Error("Unable to obtain Google access token");
  }

  const endpoint = `https://sheets.googleapis.com/v4/spreadsheets/${encodeURIComponent(
    sheetId,
  )}/values/${encodeURIComponent(SHEET_RANGE)}:append?valueInputOption=RAW&insertDataOption=INSERT_ROWS`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ values: [row] }),
  });

  if (!response.ok) {
    // Log status only — never the response body, which could echo details.
    console.error(`contact:sheets append failed status=${response.status}`);
    throw new Error(`Sheets append failed with status ${response.status}`);
  }
}
