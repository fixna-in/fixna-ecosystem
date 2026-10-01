package in.fixna.platform.consulting.meeting.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.meeting.Meeting;
import in.fixna.platform.consulting.meeting.MeetingStatus;

public record MeetingResponse(
        UUID id,
        UUID clientId,
        UUID engagementId,
        UUID projectId,
        String title,
        String description,
        String location,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        UUID organizerUserId,
        MeetingStatus status,
        String calendarProvider,
        String calendarEventId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getClientId(),
                meeting.getEngagementId(),
                meeting.getProjectId(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getLocation(),
                meeting.getStartsAt(),
                meeting.getEndsAt(),
                meeting.getOrganizerUserId(),
                meeting.getStatus(),
                meeting.getCalendarProvider(),
                meeting.getCalendarEventId(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt());
    }
}
