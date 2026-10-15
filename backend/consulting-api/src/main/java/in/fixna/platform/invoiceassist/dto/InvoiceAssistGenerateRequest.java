package in.fixna.platform.invoiceassist.dto;

public record InvoiceAssistGenerateRequest(Boolean useAi) {

    public boolean useAiRequested() {
        return Boolean.TRUE.equals(useAi);
    }
}
