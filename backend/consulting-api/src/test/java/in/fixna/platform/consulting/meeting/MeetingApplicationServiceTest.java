package in.fixna.platform.consulting.meeting;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.meeting.dto.MeetingRequest;
import in.fixna.platform.consulting.meeting.dto.MeetingResponse;
import in.fixna.platform.consulting.project.ProjectRepository;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.scheduling.CalendarProvider;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID clientId = UUID.randomUUID();

    @Mock MeetingRepository meetings;
    @Mock ClientRepository clients;
    @Mock EngagementRepository engagements;
    @Mock ProjectRepository projects;
    @Mock CalendarProvider calendarProvider;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    @InjectMocks MeetingApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultantAdmin() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);
    }

    private void asClientUser() {
        TenantContext.set(tenantId, userId, Role.CLIENT_USER);
    }

    @Test
    void createSchedulesMeetingAndPublishesEvent() {
        asConsultantAdmin();
        Client client = new Client();
        client.setId(clientId);
        client.setTenantId(tenantId);
        when(clients.findByIdAndTenantId(clientId, tenantId)).thenReturn(Optional.of(client));
        when(calendarProvider.providerId()).thenReturn("INTERNAL");
        when(calendarProvider.schedule(any())).thenReturn(UUID.randomUUID().toString());
        when(meetings.save(any(Meeting.class))).thenAnswer(invocation -> {
            Meeting meeting = invocation.getArgument(0);
            meeting.setId(UUID.randomUUID());
            return meeting;
        });

        OffsetDateTime starts = OffsetDateTime.parse("2026-10-01T10:00:00Z");
        OffsetDateTime ends = OffsetDateTime.parse("2026-10-01T11:00:00Z");
        MeetingResponse response = service.create(new MeetingRequest(
                clientId, null, null, "Kickoff", "Agenda", "Room A", starts, ends, null));

        assertThat(response.title()).isEqualTo("Kickoff");
        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.status()).isEqualTo(MeetingStatus.SCHEDULED);
        assertThat(response.calendarProvider()).isEqualTo("INTERNAL");
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void transitionMovesMeetingToInProgress() {
        asConsultantAdmin();
        UUID meetingId = UUID.randomUUID();
        Meeting existing = new Meeting();
        existing.setId(meetingId);
        existing.setTenantId(tenantId);
        existing.setClientId(clientId);
        existing.setTitle("Sync");
        existing.setOrganizerUserId(userId);
        existing.setStartsAt(OffsetDateTime.parse("2026-10-01T10:00:00Z"));
        existing.setEndsAt(OffsetDateTime.parse("2026-10-01T11:00:00Z"));
        existing.setStatus(MeetingStatus.SCHEDULED);
        existing.setCalendarProvider("INTERNAL");
        existing.setCalendarEventId(UUID.randomUUID().toString());
        when(meetings.findByIdAndTenantId(meetingId, tenantId)).thenReturn(Optional.of(existing));
        when(meetings.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeetingResponse response = service.transition(meetingId, new StatusTransitionRequest("IN_PROGRESS"));

        assertThat(response.status()).isEqualTo(MeetingStatus.IN_PROGRESS);
        verify(events).publish(any());
    }

    @Test
    void clientUserCannotScheduleMeeting() {
        asClientUser();

        assertThatThrownBy(() -> service.create(new MeetingRequest(
                        clientId,
                        null,
                        null,
                        "Blocked",
                        null,
                        null,
                        OffsetDateTime.now(),
                        OffsetDateTime.now().plusHours(1),
                        null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingMeetingReturnsNotFound() {
        asConsultantAdmin();
        UUID meetingId = UUID.randomUUID();
        when(meetings.findByIdAndTenantId(meetingId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(meetingId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }
}
