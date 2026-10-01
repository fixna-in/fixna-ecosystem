package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.project.Task;
import in.fixna.platform.consulting.project.TaskStatus;

public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID milestoneId,
        String title,
        String description,
        TaskStatus status,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getMilestoneId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
