package in.fixna.platform.consulting.publicsite.dto;

import java.math.BigDecimal;

import in.fixna.platform.consulting.service.ConsultingService;

public record PublicServiceResponse(
        String name,
        String description,
        BigDecimal priceAmount,
        String priceCurrency,
        Integer durationMinutes,
        int sortOrder) {

    public static PublicServiceResponse from(ConsultingService service) {
        return new PublicServiceResponse(
                service.getName(),
                service.getDescription(),
                service.getPriceAmount(),
                service.getPriceCurrency(),
                service.getDurationMinutes(),
                service.getSortOrder());
    }
}
