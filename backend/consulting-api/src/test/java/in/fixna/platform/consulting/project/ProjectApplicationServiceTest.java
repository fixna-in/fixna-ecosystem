package in.fixna.platform.consulting.project;

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
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.Engagement;
import in.fixna.platform.consulting.engagement.EngagementApplicationService;
import in.fixna.platform.consulting.project.dto.ProjectRequest;
import in.fixna.platform.consulting.project.dto.ProjectResponse;
import in.fixna.platform.consulting.project.dto.TaskRequest;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Role;
import in.fixna.platform.tenancy.TenantContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectApplicationServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID engagementId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();

    @Mock ProjectRepository projects;
    @Mock MilestoneRepository milestones;
    @Mock TaskRepository tasks;
    @Mock EngagementApplicationService engagementService;
    @Mock DomainEventPublisher events;
    @Mock AuditPublisher audit;

    @InjectMocks ProjectApplicationService service;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private void asConsultantAdmin() {
        TenantContext.set(tenantId, userId, Role.CONSULTANT_ADMIN);
    }

    @Test
    void createProjectPersistsAndPublishesEvent() {
        asConsultantAdmin();
        Engagement engagement = new Engagement();
        engagement.setId(engagementId);
        engagement.setTenantId(tenantId);
        when(engagementService.requireEngagement(engagementId)).thenReturn(engagement);
        when(projects.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(projectId);
            return project;
        });

        ProjectResponse response = service.createProject(
                new ProjectRequest(engagementId, "Website Redesign", "Full rebuild", null, null));

        assertThat(response.name()).isEqualTo("Website Redesign");
        assertThat(response.engagementId()).isEqualTo(engagementId);
        assertThat(response.status()).isEqualTo(ProjectStatus.PLANNED);
        verify(events).publish(any());
        verify(audit).publish(any());
    }

    @Test
    void transitionProjectMovesToInProgress() {
        asConsultantAdmin();
        Project existing = new Project();
        existing.setId(projectId);
        existing.setTenantId(tenantId);
        existing.setEngagementId(engagementId);
        existing.setName("Website Redesign");
        existing.setStatus(ProjectStatus.PLANNED);
        when(projects.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.of(existing));
        when(projects.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectResponse response =
                service.transitionProject(projectId, new StatusTransitionRequest("IN_PROGRESS"));

        assertThat(response.status()).isEqualTo(ProjectStatus.IN_PROGRESS);
        verify(events).publish(any());
    }

    @Test
    void invalidTaskTransitionThrowsBadRequest() {
        asConsultantAdmin();
        UUID taskId = UUID.randomUUID();
        Task task = new Task();
        task.setId(taskId);
        task.setTenantId(tenantId);
        task.setProjectId(projectId);
        task.setTitle("Draft spec");
        task.setStatus(TaskStatus.TODO);
        when(tasks.findByIdAndTenantId(taskId, tenantId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.transitionTask(taskId, new StatusTransitionRequest("DONE")))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("INVALID_STATUS_TRANSITION");
    }

    @Test
    void missingProjectReturnsNotFound() {
        asConsultantAdmin();
        when(projects.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProject(projectId))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void createTaskRequiresExistingProject() {
        asConsultantAdmin();
        when(projects.findByIdAndTenantId(projectId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTask(projectId, new TaskRequest("Blocked task", null, null, null)))
                .isInstanceOf(FixnaException.class)
                .extracting(ex -> ((FixnaException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
