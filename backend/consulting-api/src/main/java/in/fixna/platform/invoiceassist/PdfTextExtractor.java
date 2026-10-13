package in.fixna.platform.invoiceassist;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Native PDF text extraction with PDFBox (IA-03). Parsing is hardened
 * against malformed input (IA-08): any parse failure throws
 * {@code INVALID_FILE} and is isolated per document. OCR fallback is the
 * caller's concern — this class only produces native text per page.
 */
@Component
public class PdfTextExtractor {

    static final String EXTRACTOR_VERSION = "pdfbox-3.0.3";

    /** Text of one page (1-based page number) plus a native/OCR flag. */
    public record PageText(int pageNumber, String text, boolean fromOcr) {}

    private final int maxPages;

    public PdfTextExtractor(@Value("${fixna.invoice.max-pages:50}") int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * Extracts text page by page. Falls back to the OCR engine for pages
     * with thin native text (< 200 chars); if OCR is unavailable and a page
     * yields no text at all, the whole extraction is rejected.
     */
    public List<PageText> extract(byte[] pdfBytes, OcrEngine ocrEngine) {
        List<PageText> pages = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new in.fixna.platform.common.web.FixnaException(
                        "INVALID_FILE",
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Encrypted PDFs are not supported");
            }
            int pageCount = document.getNumberOfPages();
            if (pageCount > maxPages) {
                throw new in.fixna.platform.common.web.FixnaException(
                        "PAGE_LIMIT_EXCEEDED",
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "PDF exceeds maximum page limit of " + maxPages);
            }
            for (int i = 1; i <= pageCount; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text;
                try {
                    text = stripper.getText(document);
                } catch (IOException e) {
                    throw new in.fixna.platform.common.web.FixnaException(
                            "INVALID_FILE",
                            org.springframework.http.HttpStatus.BAD_REQUEST,
                            "Unable to read PDF text");
                }
                if (text != null && text.trim().length() >= 200) {
                    pages.add(new PageText(i, text, false));
                } else if (ocrEngine != null && ocrEngine.isAvailable()) {
                    String ocr = ocrEngine.recognize(pdfBytes, i).orElse("");
                    pages.add(new PageText(i, ocr, true));
                } else if (text != null && !text.trim().isEmpty()) {
                    pages.add(new PageText(i, text, false));
                } else {
                    throw new in.fixna.platform.common.web.FixnaException(
                            "OCR_UNAVAILABLE",
                            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                            "No native text on page " + i + " and OCR is unavailable");
                }
            }
            return pages;
        } catch (IOException e) {
            throw new in.fixna.platform.common.web.FixnaException(
                    "INVALID_FILE",
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Unable to parse PDF");
        }
    }
}