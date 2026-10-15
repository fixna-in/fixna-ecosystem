package in.fixna.platform.common.web;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * @param <T> the type of content items
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}