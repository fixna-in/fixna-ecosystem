package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MilestoneRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        LocalDate dueDate,
        Integer sortOrder) {}
