package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        UUID milestoneId,
        LocalDate dueDate) {}
