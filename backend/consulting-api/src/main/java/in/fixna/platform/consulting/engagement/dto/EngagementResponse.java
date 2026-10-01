package in.fixna.platform.consulting.engagement.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.engagement.Engagement;
import in.fixna.platform.consulting.engagement.EngagementStatus;

public record EngagementResponse(
        UUID id,
        UUID clientId,
        String title,
        String description,
        EngagementStatus status,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static EngagementResponse from(Engagement engagement) {
        return new EngagementResponse(
                engagement.getId(),
                engagement.getClientId(),
                engagement.getTitle(),
                engagement.getDescription(),
                engagement.getStatus(),
                engagement.getStartDate(),
                engagement.getEndDate(),
                engagement.getCreatedAt(),
                engagement.getUpdatedAt());
    }
}
