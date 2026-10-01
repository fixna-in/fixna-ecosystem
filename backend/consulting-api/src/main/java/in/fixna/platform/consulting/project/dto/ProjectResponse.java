package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.project.Project;
import in.fixna.platform.consulting.project.ProjectStatus;

public record ProjectResponse(
        UUID id,
        UUID engagementId,
        String name,
        String description,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getEngagementId(),
                project.getName(),
                project.getDescription(),
                project.getStatus(),
                project.getStartDate(),
                project.getEndDate(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
