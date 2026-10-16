package in.fixna.platform.hospitality.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.hospitality.auth.dto.MeResponse;
import in.fixna.platform.hospitality.tenant.TenantContext;
import in.fixna.platform.hospitality.tenant.User;
import in.fixna.platform.hospitality.tenant.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Current-user profile for the authenticated session. Identity is resolved
 * only from the JWT-derived {@link TenantContext} — never from client input.
 */
@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @Operation(summary = "Profile of the authenticated user (email, names, tenant scope)")
    @GetMapping
    public ResponseEntity<MeResponse> me() {
        UUID userId = TenantContext.requireUserId();
        User user = users.findById(userId)
                .orElseThrow(() -> new FixnaException(
                        "NOT_FOUND", HttpStatus.NOT_FOUND, "User no longer exists"));
        return ResponseEntity.ok(new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                TenantContext.requireTenantId(),
                TenantContext.currentRole().name()));
    }
}
