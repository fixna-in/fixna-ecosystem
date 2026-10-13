package in.fixna.invoiceassistant.document;

import java.util.ArrayList;
import java.util.List;

import in.fixna.invoiceassistant.document.PdfTextExtractor.PageText;

/**
 * Deterministic field extraction from page text (IA-03/IA-05). Uses simple
 * anchored patterns over concatenated text; every produced value carries
 * provenance (page, snippet) and a confidence score. Money values must parse
 * via {@link MoneyGrammar} or they are dropped (never coerced). This runs
 * fully offline — no AI required for the core intake workflow.
 */
public final class FieldExtractor {

    private FieldExtractor() {}

    public record Draft(
            String fieldKey,
            String value,
            java.math.BigDecimal moneyAmount,
            String currency,
            double confidence,
            FieldSource source,
            int page,
            String snippet) {}

    public static List<Draft> extract(List<PageText> pages) {
        List<Draft> drafts = new ArrayList<>();
        StringBuilder all = new StringBuilder();
        for (PageText page : pages) {
            all.append(page.text()).append('\n');
        }
        String text = all.toString();

        anchored(drafts, "invoice_number", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)^\\s*(?:invoice|bill)\\s*(?:no\\.?|number|#)?\\s*[:#]?\\s*([A-Za-z0-9][A-Za-z0-9\\-/]{2,})\\s*$"));
        anchored(drafts, "invoice_date", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)\\b(?:invoice\\s+date|date\\s+of\\s+issue|issued?)\\b[:\\s]+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}[./][0-9]{2}[./][0-9]{4})"));
        anchored(drafts, "due_date", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)\\b(?:due\\s+date|payment\\s+due|payable\\s+by)\\b[:\\s]+([0-9]{4}-[0-9]{2}-[0-9]{2}|[0-9]{2}[./][0-9]{2}[./][0-9]{4})"));

        // Totals: try labeled total first, then generic "total".
        money(drafts, "total", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)\\b(?:grand\\s+total|total\\s+due|amount\\s+due|balance\\s+due)\\b[^0-9\\-]{0,20}([\\d.,]+)"));
        money(drafts, "subtotal", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)\\bsub\\s*-?\\s*total\\b[^0-9\\-]{0,20}([\\d.,]+)"));
        money(drafts, "tax", text, pages,
                java.util.regex.Pattern.compile(
                        "(?im)\\b(?:tax|vat|gst)\\b[^0-9\\-]{0,20}([\\d.,]+)"));

        currency(drafts, "currency", text);
        vendor(drafts, "vendor", text, pages);
        return drafts;
    }

    private static void anchored(List<Draft> out, String key, String text, List<PageText> pages,
            java.util.regex.Pattern pattern) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            int page = pageOf(text, matcher.start(), pages);
            out.add(new Draft(key, value, null, null, 0.92, FieldSource.NATIVE_TEXT, page,
                    snippetAround(text, matcher.start())));
        }
    }

    private static void money(List<Draft> out, String key, String text, List<PageText> pages,
            java.util.regex.Pattern pattern) {
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String raw = matcher.group(1);
            MoneyGrammar.parse(raw).ifPresent(amount -> {
                int page = pageOf(text, matcher.start(), pages);
                out.add(new Draft(key, amount.toPlainString(), amount, null, 0.90,
                        FieldSource.NATIVE_TEXT, page, snippetAround(text, matcher.start())));
            });
        }
    }

    private static void currency(List<Draft> out, String key, String text) {
        String upper = text.toUpperCase(java.util.Locale.ROOT);
        String code = null;
        if (upper.contains("EUR") || text.contains("€")) {
            code = "EUR";
        } else if (upper.contains("USD") || text.contains("$")) {
            code = "USD";
        } else if (upper.contains("GBP") || text.contains("£")) {
            code = "GBP";
        }
        if (code != null) {
            out.add(new Draft(key, code, null, code, 0.85, FieldSource.NATIVE_TEXT, 1, code));
        }
    }

    private static void vendor(List<Draft> out, String key, String text, List<PageText> pages) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?im)^\\s*(?:from|vendor|supplier|bill\\s+from)\\s*:\\s*(.+?)\\s*$")
                .matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (!value.isEmpty()) {
                int page = pageOf(text, matcher.start(), pages);
                out.add(new Draft(key, value, null, null, 0.88, FieldSource.NATIVE_TEXT, page,
                        snippetAround(text, matcher.start())));
            }
        }
    }

    private static int pageOf(String text, int index, List<PageText> pages) {
        int consumed = 0;
        for (PageText page : pages) {
            consumed += page.text().length() + 1;
            if (index < consumed) {
                return page.pageNumber();
            }
        }
        return 1;
    }

    private static String snippetAround(String text, int index) {
        int start = Math.max(0, index - 60);
        int end = Math.min(text.length(), index + 60);
        String snippet = text.substring(start, end).replaceAll("\\s+", " ").trim();
        return snippet.length() > 160 ? snippet.substring(0, 160) : snippet;
    }
}
