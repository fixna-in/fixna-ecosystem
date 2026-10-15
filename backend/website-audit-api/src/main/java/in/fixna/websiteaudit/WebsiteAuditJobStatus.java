package in.fixna.websiteaudit;

/** Lifecycle of an asynchronous website audit job (ADR-009). */
public enum WebsiteAuditJobStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}