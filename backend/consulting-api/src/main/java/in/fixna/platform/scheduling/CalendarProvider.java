package in.fixna.platform.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface CalendarProvider {

    String providerId();

    String schedule(CalendarSlot slot);

    void reschedule(String calendarEventId, CalendarSlot slot);

    void cancel(String calendarEventId);

    record CalendarSlot(
            UUID tenantId,
            UUID organizerUserId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            UUID excludeMeetingId) {

        public CalendarSlot(UUID tenantId, UUID organizerUserId, OffsetDateTime startsAt, OffsetDateTime endsAt) {
            this(tenantId, organizerUserId, startsAt, endsAt, null);
        }
    }
}
