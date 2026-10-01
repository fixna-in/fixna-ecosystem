package in.fixna.platform.consulting.profile;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.consulting.profile.dto.ProfileResponse;
import in.fixna.platform.consulting.profile.dto.ProfileUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "profile", description = "Consulting workspace profile")
public class ProfileController {

    private final ProfileApplicationService profileService;

    public ProfileController(ProfileApplicationService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Get consulting profile for the current tenant")
    @GetMapping
    public ResponseEntity<ProfileResponse> get() {
        return ResponseEntity.ok(profileService.get());
    }

    @Operation(summary = "Update consulting profile")
    @PutMapping
    public ResponseEntity<ProfileResponse> update(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(profileService.update(request));
    }
}
