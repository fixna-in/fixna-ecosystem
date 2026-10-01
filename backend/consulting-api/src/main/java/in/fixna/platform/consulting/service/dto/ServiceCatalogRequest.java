package in.fixna.platform.consulting.service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceCatalogRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        BigDecimal priceAmount,
        @Size(max = 3) String priceCurrency,
        Integer durationMinutes,
        boolean active,
        int sortOrder) {}
