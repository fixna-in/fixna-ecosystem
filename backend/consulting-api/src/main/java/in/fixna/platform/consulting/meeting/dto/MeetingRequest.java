package in.fixna.platform.consulting.meeting.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MeetingRequest(
        @NotNull UUID clientId,
        UUID engagementId,
        UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @Size(max = 500) String location,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
        UUID organizerUserId) {}
