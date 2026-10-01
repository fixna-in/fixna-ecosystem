package in.fixna.platform.ai.dto;

public record AiSuggestionResponse<T>(String disclaimer, T suggestion) {

    public static final String ADVISORY_DISCLAIMER = "Advisory only — review before use.";

    public static <T> AiSuggestionResponse<T> of(T suggestion) {
        return new AiSuggestionResponse<>(ADVISORY_DISCLAIMER, suggestion);
    }
}
