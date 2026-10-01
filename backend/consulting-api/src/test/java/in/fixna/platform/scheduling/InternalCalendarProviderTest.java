package in.fixna.platform.scheduling;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.meeting.MeetingRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCalendarProviderTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID organizerId = UUID.randomUUID();

    @Mock MeetingRepository meetings;

    @InjectMocks InternalCalendarProvider provider;

    @Test
    void scheduleReturnsEventIdWhenSlotIsValid() {
        OffsetDateTime starts = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        OffsetDateTime ends = OffsetDateTime.parse("2026-10-01T11:00:00Z");
        when(meetings.existsOrganizerConflict(eq(tenantId), eq(organizerId), eq(starts), eq(ends), isNull()))
                .thenReturn(false);

        String eventId = provider.schedule(new CalendarProvider.CalendarSlot(tenantId, organizerId, starts, ends));

        assertThat(eventId).isNotBlank();
    }

    @Test
    void scheduleRejectsEndsBeforeStarts() {
        OffsetDateTime starts = OffsetDateTime.parse("2026-10-01T11:00:00Z");
        OffsetDateTime ends = OffsetDateTime.parse("2026-10-01T10:00:00Z");

        assertThatThrownBy(() -> provider.schedule(new CalendarProvider.CalendarSlot(tenantId, organizerId, starts, ends)))
                .isInstanceOf(FixnaException.class)
                .satisfies(ex -> {
                    FixnaException fixna = (FixnaException) ex;
                    assertThat(fixna.getCode()).isEqualTo("INVALID_TIME_RANGE");
                    assertThat(fixna.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void scheduleRejectsOrganizerConflict() {
        OffsetDateTime starts = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        OffsetDateTime ends = OffsetDateTime.parse("2026-10-01T11:00:00Z");
        when(meetings.existsOrganizerConflict(any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> provider.schedule(new CalendarProvider.CalendarSlot(tenantId, organizerId, starts, ends)))
                .isInstanceOf(FixnaException.class)
                .satisfies(ex -> {
                    FixnaException fixna = (FixnaException) ex;
                    assertThat(fixna.getCode()).isEqualTo("CALENDAR_CONFLICT");
                    assertThat(fixna.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void rescheduleAllowsExcludingCurrentMeeting() {
        OffsetDateTime starts = OffsetDateTime.parse("2026-10-01T12:00:00Z");
        OffsetDateTime ends = OffsetDateTime.parse("2026-10-01T13:00:00Z");
        UUID meetingId = UUID.randomUUID();
        when(meetings.existsOrganizerConflict(eq(tenantId), eq(organizerId), eq(starts), eq(ends), eq(meetingId)))
                .thenReturn(false);

        assertThatCode(() -> provider.reschedule(
                        "event-1",
                        new CalendarProvider.CalendarSlot(tenantId, organizerId, starts, ends, meetingId)))
                .doesNotThrowAnyException();
    }
}
