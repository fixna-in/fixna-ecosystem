package in.fixna.platform.consulting.project;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.project.dto.MilestoneRequest;
import in.fixna.platform.consulting.project.dto.MilestoneResponse;
import in.fixna.platform.consulting.project.dto.ProjectRequest;
import in.fixna.platform.consulting.project.dto.ProjectResponse;
import in.fixna.platform.consulting.project.dto.TaskRequest;
import in.fixna.platform.consulting.project.dto.TaskResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "projects", description = "Projects, milestones, and tasks")
public class ProjectController {

    private final ProjectApplicationService projectService;

    public ProjectController(ProjectApplicationService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "List projects for the current tenant")
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }

    @Operation(summary = "Create a project")
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @Operation(summary = "Get project by id")
    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }

    @Operation(summary = "Update project")
    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID id, @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @Operation(summary = "Transition project status")
    @PostMapping("/projects/{id}/transition")
    public ResponseEntity<ProjectResponse> transitionProject(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(projectService.transitionProject(id, request));
    }

    @Operation(summary = "List milestones for a project")
    @GetMapping("/projects/{id}/milestones")
    public ResponseEntity<List<MilestoneResponse>> listMilestones(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.listMilestones(id));
    }

    @Operation(summary = "Create a milestone")
    @PostMapping("/projects/{id}/milestones")
    public ResponseEntity<MilestoneResponse> createMilestone(
            @PathVariable UUID id, @Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createMilestone(id, request));
    }

    @Operation(summary = "Transition milestone status")
    @PostMapping("/milestones/{id}/transition")
    public ResponseEntity<MilestoneResponse> transitionMilestone(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(projectService.transitionMilestone(id, request));
    }

    @Operation(summary = "List tasks for a project")
    @GetMapping("/projects/{id}/tasks")
    public ResponseEntity<List<TaskResponse>> listTasks(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.listTasks(id));
    }

    @Operation(summary = "Create a task")
    @PostMapping("/projects/{id}/tasks")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createTask(id, request));
    }

    @Operation(summary = "Update a task")
    @PutMapping("/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(projectService.updateTask(id, request));
    }

    @Operation(summary = "Transition task status")
    @PostMapping("/tasks/{id}/transition")
    public ResponseEntity<TaskResponse> transitionTask(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(projectService.transitionTask(id, request));
    }
}
