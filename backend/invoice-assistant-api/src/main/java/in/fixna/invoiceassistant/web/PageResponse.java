package in.fixna.invoiceassistant.web;

import java.util.List;

/** Generic paginated response wrapper. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
