package in.fixna.platform.consulting.service.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.service.ConsultingService;

public record ServiceCatalogResponse(
        UUID id,
        String name,
        String description,
        BigDecimal priceAmount,
        String priceCurrency,
        Integer durationMinutes,
        boolean active,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ServiceCatalogResponse from(ConsultingService service) {
        return new ServiceCatalogResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getPriceAmount(),
                service.getPriceCurrency(),
                service.getDurationMinutes(),
                service.isActive(),
                service.getSortOrder(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
