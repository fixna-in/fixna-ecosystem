package in.fixna.platform.invoiceassist;

import java.util.Optional;

/**
 * OCR adapter (IA-04). Implementations turn page images/raster into text.
 * The default engine is offline-capable so the core intake pipeline never
 * depends on a cloud service; when no engine is configured, callers get an
 * empty result and the service surfaces {@code OCR_UNAVAILABLE} for
 * pages that have no native text.
 */
public interface OcrEngine {

    /** True when a working engine is configured. */
    boolean isAvailable();

    /**
     * Runs OCR over the given page text source. Implementations must run
     * offline and must not retain page images. Returns empty when the engine
     * is unavailable or produces no text.
     */
    Optional<String> recognize(byte[] pageBytes, int pageNumber);
}