package in.fixna.invoiceassistant.document;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Offline no-op OCR engine for the MVP (IA-04). A real offline engine
 * (e.g. Tesseract bindings) can replace this bean later; the extraction
 * service already handles the unavailable path with {@code OCR_UNAVAILABLE}.
 */
@Component
public class NoopOcrEngine implements OcrEngine {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<String> recognize(byte[] pageBytes, int pageNumber) {
        return Optional.empty();
    }
}
