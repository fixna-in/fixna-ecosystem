package in.fixna.platform.consulting.meeting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.Engagement;
import in.fixna.platform.consulting.engagement.EngagementRepository;
import in.fixna.platform.consulting.meeting.dto.MeetingRequest;
import in.fixna.platform.consulting.meeting.dto.MeetingResponse;
import in.fixna.platform.consulting.meeting.event.MeetingScheduled;
import in.fixna.platform.consulting.meeting.event.MeetingStatusChanged;
import in.fixna.platform.consulting.project.Project;
import in.fixna.platform.consulting.project.ProjectRepository;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.scheduling.CalendarProvider;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class MeetingApplicationService {

    private final MeetingRepository meetings;
    private final ClientRepository clients;
    private final EngagementRepository engagements;
    private final ProjectRepository projects;
    private final CalendarProvider calendarProvider;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public MeetingApplicationService(
            MeetingRepository meetings,
            ClientRepository clients,
            EngagementRepository engagements,
            ProjectRepository projects,
            CalendarProvider calendarProvider,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.meetings = meetings;
        this.clients = clients;
        this.engagements = engagements;
        this.projects = projects;
        this.calendarProvider = calendarProvider;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(UUID clientId, UUID projectId) {
        TenantContext.requirePermission(Permission.MEETING_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        List<Meeting> results;
        if (clientId != null && projectId != null) {
            results = meetings.findByTenantIdAndClientIdAndProjectIdOrderByStartsAtAsc(
                    tenantId, clientId, projectId);
        } else if (clientId != null) {
            results = meetings.findByTenantIdAndClientIdOrderByStartsAtAsc(tenantId, clientId);
        } else if (projectId != null) {
            results = meetings.findByTenantIdAndProjectIdOrderByStartsAtAsc(tenantId, projectId);
        } else {
            results = meetings.findByTenantIdOrderByStartsAtAsc(tenantId);
        }
        return results.stream().map(MeetingResponse::from).toList();
    }

    @Transactional
    public MeetingResponse create(MeetingRequest request) {
        TenantContext.requirePermission(Permission.MEETING_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        UUID organizerUserId = request.organizerUserId() != null
                ? request.organizerUserId()
                : TenantContext.requireUserId();
        validateLinks(tenantId, request.clientId(), request.engagementId(), request.projectId());

        Meeting meeting = new Meeting();
        meeting.setTenantId(tenantId);
        applyFields(meeting, request, organizerUserId);

        CalendarProvider.CalendarSlot slot = calendarSlot(meeting, null);
        String eventId = calendarProvider.schedule(slot);
        meeting.setCalendarProvider(calendarProvider.providerId());
        meeting.setCalendarEventId(eventId);
        meetings.save(meeting);

        events.publish(new MeetingScheduled(meeting.getId(), tenantId));
        audit.publish(new AuditEvent(
                "meeting.scheduled",
                tenantId,
                TenantContext.requireUserId(),
                "meeting",
                meeting.getId().toString(),
                Map.of("title", meeting.getTitle(), "clientId", request.clientId().toString()),
                null));
        return MeetingResponse.from(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(UUID id) {
        TenantContext.requirePermission(Permission.MEETING_VIEW);
        return MeetingResponse.from(requireMeeting(id));
    }

    @Transactional
    public MeetingResponse update(UUID id, MeetingRequest request) {
        TenantContext.requirePermission(Permission.MEETING_MANAGE);
        Meeting meeting = requireMeeting(id);
        UUID tenantId = meeting.getTenantId();
        UUID organizerUserId = request.organizerUserId() != null
                ? request.organizerUserId()
                : meeting.getOrganizerUserId();
        validateLinks(tenantId, request.clientId(), request.engagementId(), request.projectId());

        boolean timeOrOrganizerChanged = !meeting.getStartsAt().equals(request.startsAt())
                || !meeting.getEndsAt().equals(request.endsAt())
                || !meeting.getOrganizerUserId().equals(organizerUserId);

        applyFields(meeting, request, organizerUserId);
        if (timeOrOrganizerChanged) {
            CalendarProvider.CalendarSlot slot = calendarSlot(meeting, meeting.getId());
            calendarProvider.reschedule(meeting.getCalendarEventId(), slot);
        }
        meetings.save(meeting);

        audit.publish(new AuditEvent(
                "meeting.updated",
                tenantId,
                TenantContext.requireUserId(),
                "meeting",
                meeting.getId().toString(),
                Map.of("title", meeting.getTitle()),
                null));
        return MeetingResponse.from(meeting);
    }

    @Transactional
    public MeetingResponse transition(UUID id, StatusTransitionRequest request) {
        TenantContext.requirePermission(Permission.MEETING_MANAGE);
        Meeting meeting = requireMeeting(id);
        MeetingStatus fromStatus = meeting.getStatus();
        MeetingStatus toStatus = parseMeetingStatus(request.status());
        fromStatus.validateTransitionTo(toStatus);
        meeting.setStatus(toStatus);
        if (toStatus == MeetingStatus.CANCELLED) {
            calendarProvider.cancel(meeting.getCalendarEventId());
        }
        meetings.save(meeting);

        events.publish(new MeetingStatusChanged(
                meeting.getId(), meeting.getTenantId(), fromStatus, toStatus));
        audit.publish(new AuditEvent(
                "meeting.status_changed",
                meeting.getTenantId(),
                TenantContext.requireUserId(),
                "meeting",
                meeting.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return MeetingResponse.from(meeting);
    }

    public Meeting requireMeeting(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return meetings.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Meeting not found"));
    }

    private void validateLinks(UUID tenantId, UUID clientId, UUID engagementId, UUID projectId) {
        clients.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));

        if (engagementId != null) {
            Engagement engagement = engagements.findByIdAndTenantId(engagementId, tenantId)
                    .orElseThrow(() -> new FixnaException(
                            "NOT_FOUND", HttpStatus.NOT_FOUND, "Engagement not found"));
            if (!engagement.getClientId().equals(clientId)) {
                throw new FixnaException(
                        "NOT_FOUND", HttpStatus.NOT_FOUND, "Engagement not found for client");
            }
        }

        if (projectId != null) {
            Project project = projects.findByIdAndTenantId(projectId, tenantId)
                    .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Project not found"));
            if (engagementId != null && !project.getEngagementId().equals(engagementId)) {
                throw new FixnaException(
                        "NOT_FOUND", HttpStatus.NOT_FOUND, "Project not found for engagement");
            }
            if (engagementId == null) {
                Engagement engagement = engagements
                        .findByIdAndTenantId(project.getEngagementId(), tenantId)
                        .orElseThrow(() -> new FixnaException(
                                "NOT_FOUND", HttpStatus.NOT_FOUND, "Engagement not found"));
                if (!engagement.getClientId().equals(clientId)) {
                    throw new FixnaException(
                            "NOT_FOUND", HttpStatus.NOT_FOUND, "Project not found for client");
                }
            }
        }
    }

    private static void applyFields(Meeting meeting, MeetingRequest request, UUID organizerUserId) {
        meeting.setClientId(request.clientId());
        meeting.setEngagementId(request.engagementId());
        meeting.setProjectId(request.projectId());
        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setLocation(request.location());
        meeting.setStartsAt(request.startsAt());
        meeting.setEndsAt(request.endsAt());
        meeting.setOrganizerUserId(organizerUserId);
    }

    private static CalendarProvider.CalendarSlot calendarSlot(Meeting meeting, UUID excludeMeetingId) {
        return new CalendarProvider.CalendarSlot(
                meeting.getTenantId(),
                meeting.getOrganizerUserId(),
                meeting.getStartsAt(),
                meeting.getEndsAt(),
                excludeMeetingId);
    }

    private static MeetingStatus parseMeetingStatus(String raw) {
        try {
            return MeetingStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new FixnaException(
                    "INVALID_STATUS", HttpStatus.BAD_REQUEST, "Unknown meeting status: " + raw);
        }
    }
}
