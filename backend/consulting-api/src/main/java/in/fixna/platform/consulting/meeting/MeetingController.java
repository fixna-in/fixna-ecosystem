package in.fixna.platform.consulting.meeting;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.dto.StatusTransitionRequest;
import in.fixna.platform.consulting.meeting.dto.MeetingRequest;
import in.fixna.platform.consulting.meeting.dto.MeetingResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/meetings")
@Tag(name = "meetings", description = "Meetings and calendar scheduling")
public class MeetingController {

    private final MeetingApplicationService meetingService;

    public MeetingController(MeetingApplicationService meetingService) {
        this.meetingService = meetingService;
    }

    @Operation(summary = "List meetings")
    @GetMapping
    public ResponseEntity<List<MeetingResponse>> list(
            @RequestParam(required = false) UUID clientId, @RequestParam(required = false) UUID projectId) {
        return ResponseEntity.ok(meetingService.list(clientId, projectId));
    }

    @Operation(summary = "Schedule a meeting")
    @PostMapping
    public ResponseEntity<MeetingResponse> create(@Valid @RequestBody MeetingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingService.create(request));
    }

    @Operation(summary = "Get meeting by id")
    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(meetingService.get(id));
    }

    @Operation(summary = "Update meeting")
    @PutMapping("/{id}")
    public ResponseEntity<MeetingResponse> update(@PathVariable UUID id, @Valid @RequestBody MeetingRequest request) {
        return ResponseEntity.ok(meetingService.update(id, request));
    }

    @Operation(summary = "Transition meeting status")
    @PostMapping("/{id}/transition")
    public ResponseEntity<MeetingResponse> transition(
            @PathVariable UUID id, @Valid @RequestBody StatusTransitionRequest request) {
        return ResponseEntity.ok(meetingService.transition(id, request));
    }
}
