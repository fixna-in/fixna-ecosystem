package in.fixna.invoiceassistant.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import in.fixna.invoiceassistant.document.PdfTextExtractor.PageText;

class FieldExtractorTest {

    private static final String INVOICE_TEXT = """
            From: ACME Supplies Ltd
            Invoice Number: INV-2024-0042
            Invoice date: 2024-05-01
            Due date: 01/06/2024

            Subtotal 100.00
            Tax 21.00
            Grand Total: EUR 121.00
            """;

    private static Map<String, FieldExtractor.Draft> extractByKey(String text) {
        List<FieldExtractor.Draft> drafts =
                FieldExtractor.extract(List.of(new PageText(1, text, false)));
        return drafts.stream()
                .collect(Collectors.toMap(FieldExtractor.Draft::fieldKey, Function.identity()));
    }

    @Test
    void extractsCoreInvoiceFieldsWithProvenance() {
        Map<String, FieldExtractor.Draft> byKey = extractByKey(INVOICE_TEXT);

        assertThat(byKey).containsKeys("vendor", "invoice_number", "invoice_date", "due_date",
                "subtotal", "tax", "total", "currency");
        assertThat(byKey.get("invoice_number").value()).isEqualTo("INV-2024-0042");
        assertThat(byKey.get("invoice_date").value()).isEqualTo("2024-05-01");
        assertThat(byKey.get("due_date").value()).isEqualTo("01/06/2024");
        assertThat(byKey.get("vendor").value()).isEqualTo("ACME Supplies Ltd");
        assertThat(byKey.get("total").moneyAmount()).isEqualByComparingTo("121.00");
        assertThat(byKey.get("currency").currency()).isEqualTo("EUR");
    }

    @Test
    void everyDraftCarriesNativeProvenanceAndPage() {
        for (FieldExtractor.Draft draft : FieldExtractor.extract(
                List.of(new PageText(1, INVOICE_TEXT, false)))) {
            assertThat(draft.source()).isEqualTo(FieldSource.NATIVE_TEXT);
            assertThat(draft.page()).isEqualTo(1);
            assertThat(draft.snippet()).isNotBlank();
        }
    }

    @Test
    void dropsUnparsableMoneyInsteadOfCoercing() {
        Map<String, FieldExtractor.Draft> byKey =
                extractByKey("Grand Total: not-a-number at all");
        assertThat(byKey).doesNotContainKey("total");
    }
}
