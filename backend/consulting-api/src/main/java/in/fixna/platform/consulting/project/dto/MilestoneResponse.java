package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.project.Milestone;
import in.fixna.platform.consulting.project.MilestoneStatus;

public record MilestoneResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        MilestoneStatus status,
        LocalDate dueDate,
        int sortOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static MilestoneResponse from(Milestone milestone) {
        return new MilestoneResponse(
                milestone.getId(),
                milestone.getProjectId(),
                milestone.getName(),
                milestone.getDescription(),
                milestone.getStatus(),
                milestone.getDueDate(),
                milestone.getSortOrder(),
                milestone.getCreatedAt(),
                milestone.getUpdatedAt());
    }
}
