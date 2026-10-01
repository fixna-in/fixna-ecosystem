package in.fixna.platform.consulting.project.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotNull UUID engagementId,
        @NotBlank @Size(max = 255) String name,
        String description,
        LocalDate startDate,
        LocalDate endDate) {}
