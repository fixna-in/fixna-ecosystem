package in.fixna.platform.scheduling;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.meeting.MeetingRepository;

@Component
public class InternalCalendarProvider implements CalendarProvider {

    public static final String PROVIDER_ID = "INTERNAL";

    private final MeetingRepository meetings;

    public InternalCalendarProvider(MeetingRepository meetings) {
        this.meetings = meetings;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public String schedule(CalendarSlot slot) {
        validateTimeRange(slot);
        assertNoConflict(slot);
        return UUID.randomUUID().toString();
    }

    @Override
    public void reschedule(String calendarEventId, CalendarSlot slot) {
        if (calendarEventId == null || calendarEventId.isBlank()) {
            throw new FixnaException(
                    "CALENDAR_EVENT_MISSING",
                    HttpStatus.BAD_REQUEST,
                    "Calendar event id is required to reschedule");
        }
        validateTimeRange(slot);
        assertNoConflict(slot);
    }

    @Override
    public void cancel(String calendarEventId) {
        if (calendarEventId == null || calendarEventId.isBlank()) {
            return;
        }
    }

    private static void validateTimeRange(CalendarSlot slot) {
        if (!slot.endsAt().isAfter(slot.startsAt())) {
            throw new FixnaException(
                    "INVALID_TIME_RANGE",
                    HttpStatus.BAD_REQUEST,
                    "Meeting end time must be after start time");
        }
    }

    private void assertNoConflict(CalendarSlot slot) {
        boolean conflict = meetings.existsOrganizerConflict(
                slot.tenantId(),
                slot.organizerUserId(),
                slot.startsAt(),
                slot.endsAt(),
                slot.excludeMeetingId());
        if (conflict) {
            throw new FixnaException(
                    "CALENDAR_CONFLICT",
                    HttpStatus.CONFLICT,
                    "Organizer has a conflicting meeting in this time range");
        }
    }
}
