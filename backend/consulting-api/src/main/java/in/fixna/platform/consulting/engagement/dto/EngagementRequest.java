package in.fixna.platform.consulting.engagement.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EngagementRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate startDate,
        LocalDate endDate) {}
