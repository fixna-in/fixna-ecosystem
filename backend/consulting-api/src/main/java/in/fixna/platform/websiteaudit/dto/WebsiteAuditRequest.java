package in.fixna.platform.websiteaudit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebsiteAuditRequest(
        @NotBlank @Size(max = 2000) String url,
        Boolean crawl) {

    /** Convenience constructor: crawl defaults to enabled. */
    public WebsiteAuditRequest(String url) {
        this(url, Boolean.TRUE);
    }

    /** Crawl defaults to enabled when omitted (bounds are server-enforced). */
    public boolean crawlEnabled() {
        return crawl == null || crawl;
    }
}
