package in.fixna.platform.consulting.meeting.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.meeting.MeetingStatus;
import in.fixna.platform.event.DomainEvent;

public record MeetingStatusChanged(
        UUID meetingId,
        UUID tenantId,
        MeetingStatus fromStatus,
        MeetingStatus toStatus,
        OffsetDateTime occurredAt)
        implements DomainEvent {

    public MeetingStatusChanged(
            UUID meetingId, UUID tenantId, MeetingStatus fromStatus, MeetingStatus toStatus) {
        this(meetingId, tenantId, fromStatus, toStatus, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "meeting.status_changed";
    }

    @Override
    public UUID tenantId() {
        return tenantId;
    }

    @Override
    public OffsetDateTime occurredAt() {
        return occurredAt;
    }
}
