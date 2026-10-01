package in.fixna.platform.consulting.project;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.engagement.EngagementApplicationService;
import in.fixna.platform.consulting.project.dto.MilestoneRequest;
import in.fixna.platform.consulting.project.dto.MilestoneResponse;
import in.fixna.platform.consulting.project.dto.ProjectRequest;
import in.fixna.platform.consulting.project.dto.ProjectResponse;
import in.fixna.platform.consulting.project.dto.TaskRequest;
import in.fixna.platform.consulting.project.dto.TaskResponse;
import in.fixna.platform.consulting.project.event.ProjectCreated;
import in.fixna.platform.consulting.project.event.ProjectStatusChanged;
import in.fixna.platform.event.DomainEventPublisher;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class ProjectApplicationService {

    private final ProjectRepository projects;
    private final MilestoneRepository milestones;
    private final TaskRepository tasks;
    private final EngagementApplicationService engagementService;
    private final DomainEventPublisher events;
    private final AuditPublisher audit;

    public ProjectApplicationService(
            ProjectRepository projects,
            MilestoneRepository milestones,
            TaskRepository tasks,
            EngagementApplicationService engagementService,
            DomainEventPublisher events,
            AuditPublisher audit) {
        this.projects = projects;
        this.milestones = milestones;
        this.tasks = tasks;
        this.engagementService = engagementService;
        this.events = events;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listProjects() {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        UUID tenantId = TenantContext.requireTenantId();
        return projects.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        UUID tenantId = TenantContext.requireTenantId();
        engagementService.requireEngagement(request.engagementId());

        Project project = new Project();
        project.setTenantId(tenantId);
        project.setEngagementId(request.engagementId());
        applyProjectFields(project, request);
        projects.save(project);

        events.publish(new ProjectCreated(project.getId(), tenantId));
        audit.publish(new AuditEvent(
                "project.created",
                tenantId,
                TenantContext.requireUserId(),
                "project",
                project.getId().toString(),
                Map.of("name", project.getName()),
                null));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID id) {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        return ProjectResponse.from(requireProject(id));
    }

    @Transactional
    public ProjectResponse updateProject(UUID id, ProjectRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Project project = requireProject(id);
        if (!project.getEngagementId().equals(request.engagementId())) {
            engagementService.requireEngagement(request.engagementId());
            project.setEngagementId(request.engagementId());
        }
        applyProjectFields(project, request);
        projects.save(project);

        audit.publish(new AuditEvent(
                "project.updated",
                project.getTenantId(),
                TenantContext.requireUserId(),
                "project",
                project.getId().toString(),
                Map.of("name", project.getName()),
                null));
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse transitionProject(UUID id, StatusTransitionRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Project project = requireProject(id);
        ProjectStatus fromStatus = project.getStatus();
        ProjectStatus toStatus = parseProjectStatus(request.status());
        fromStatus.validateTransitionTo(toStatus);
        project.setStatus(toStatus);
        projects.save(project);

        events.publish(new ProjectStatusChanged(
                project.getId(), project.getTenantId(), fromStatus, toStatus));
        audit.publish(new AuditEvent(
                "project.status_changed",
                project.getTenantId(),
                TenantContext.requireUserId(),
                "project",
                project.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<MilestoneResponse> listMilestones(UUID projectId) {
        TenantContext.requirePermission(Permission.PROJECT_VIEW);
        Project project = requireProject(projectId);
        return milestones
                .findByProjectIdAndTenantIdOrderBySortOrderAsc(project.getId(), project.getTenantId())
                .stream()
                .map(MilestoneResponse::from)
                .toList();
    }

    @Transactional
    public MilestoneResponse createMilestone(UUID projectId, MilestoneRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Project project = requireProject(projectId);

        Milestone milestone = new Milestone();
        milestone.setTenantId(project.getTenantId());
        milestone.setProjectId(project.getId());
        milestone.setName(request.name());
        milestone.setDescription(request.description());
        milestone.setDueDate(request.dueDate());
        milestone.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        milestones.save(milestone);

        audit.publish(new AuditEvent(
                "milestone.created",
                project.getTenantId(),
                TenantContext.requireUserId(),
                "milestone",
                milestone.getId().toString(),
                Map.of("name", milestone.getName()),
                null));
        return MilestoneResponse.from(milestone);
    }

    @Transactional
    public MilestoneResponse transitionMilestone(UUID id, StatusTransitionRequest request) {
        TenantContext.requirePermission(Permission.PROJECT_MANAGE);
        Milestone milestone = requireMilestone(id);
        MilestoneStatus fromStatus = milestone.getStatus();
        MilestoneStatus toStatus = parseMilestoneStatus(request.status());
        fromStatus.validateTransitionTo(toStatus);
        milestone.setStatus(toStatus);
        milestones.save(milestone);

        audit.publish(new AuditEvent(
                "milestone.status_changed",
                milestone.getTenantId(),
                TenantContext.requireUserId(),
                "milestone",
                milestone.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return MilestoneResponse.from(milestone);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(UUID projectId) {
        TenantContext.requirePermission(Permission.TASK_VIEW);
        Project project = requireProject(projectId);
        return tasks.findByProjectIdAndTenantIdOrderByCreatedAtAsc(project.getId(), project.getTenantId()).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(UUID projectId, TaskRequest request) {
        TenantContext.requirePermission(Permission.TASK_MANAGE);
        Project project = requireProject(projectId);
        validateMilestoneForProject(request.milestoneId(), project);

        Task task = new Task();
        task.setTenantId(project.getTenantId());
        task.setProjectId(project.getId());
        task.setMilestoneId(request.milestoneId());
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        tasks.save(task);

        audit.publish(new AuditEvent(
                "task.created",
                project.getTenantId(),
                TenantContext.requireUserId(),
                "task",
                task.getId().toString(),
                Map.of("title", task.getTitle()),
                null));
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskRequest request) {
        TenantContext.requirePermission(Permission.TASK_MANAGE);
        Task task = requireTask(id);
        Project project = requireProject(task.getProjectId());
        validateMilestoneForProject(request.milestoneId(), project);

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setMilestoneId(request.milestoneId());
        task.setDueDate(request.dueDate());
        tasks.save(task);

        audit.publish(new AuditEvent(
                "task.updated",
                task.getTenantId(),
                TenantContext.requireUserId(),
                "task",
                task.getId().toString(),
                Map.of("title", task.getTitle()),
                null));
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse transitionTask(UUID id, StatusTransitionRequest request) {
        TenantContext.requirePermission(Permission.TASK_MANAGE);
        Task task = requireTask(id);
        TaskStatus fromStatus = task.getStatus();
        TaskStatus toStatus = parseTaskStatus(request.status());
        fromStatus.validateTransitionTo(toStatus);
        task.setStatus(toStatus);
        tasks.save(task);

        audit.publish(new AuditEvent(
                "task.status_changed",
                task.getTenantId(),
                TenantContext.requireUserId(),
                "task",
                task.getId().toString(),
                Map.of("fromStatus", fromStatus.name(), "toStatus", toStatus.name()),
                null));
        return TaskResponse.from(task);
    }

    Project requireProject(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return projects.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Project not found"));
    }

    private Milestone requireMilestone(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return milestones.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Milestone not found"));
    }

    private Task requireTask(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return tasks.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Task not found"));
    }

    private void validateMilestoneForProject(UUID milestoneId, Project project) {
        if (milestoneId == null) {
            return;
        }
        Milestone milestone = requireMilestone(milestoneId);
        if (!milestone.getProjectId().equals(project.getId())) {
            throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Milestone not found");
        }
    }

    private static void applyProjectFields(Project project, ProjectRequest request) {
        project.setName(request.name());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }

    private static ProjectStatus parseProjectStatus(String raw) {
        try {
            return ProjectStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new FixnaException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "Unknown project status: " + raw);
        }
    }

    private static MilestoneStatus parseMilestoneStatus(String raw) {
        try {
            return MilestoneStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new FixnaException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "Unknown milestone status: " + raw);
        }
    }

    private static TaskStatus parseTaskStatus(String raw) {
        try {
            return TaskStatus.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw new FixnaException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "Unknown task status: " + raw);
        }
    }
}
