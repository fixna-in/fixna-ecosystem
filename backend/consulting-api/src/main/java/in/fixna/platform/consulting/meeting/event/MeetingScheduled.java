package in.fixna.platform.consulting.meeting.event;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.event.DomainEvent;

public record MeetingScheduled(UUID meetingId, UUID tenantId, OffsetDateTime occurredAt) implements DomainEvent {

    public MeetingScheduled(UUID meetingId, UUID tenantId) {
        this(meetingId, tenantId, OffsetDateTime.now());
    }

    @Override
    public String eventName() {
        return "meeting.scheduled";
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
