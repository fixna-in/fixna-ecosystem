package in.fixna.invoiceassistant.auth;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.invoiceassistant.config.TenantContext;
import in.fixna.invoiceassistant.tenant.User;
import in.fixna.invoiceassistant.tenant.UserRepository;
import in.fixna.platform.common.web.FixnaException;

import io.swagger.v3.oas.annotations.Operation;

/** Current authenticated user profile. */
@RestController
@RequestMapping("/api/v1")
public class MeController {

    public record MeResponse(UUID id, String email, String firstName, String lastName) {}

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
    }

    @Operation(summary = "Current authenticated user")
    @GetMapping("/me")
    public MeResponse me() {
        UUID userId = TenantContext.requireUserId();
        User user = users.findById(userId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "User not found"));
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }
}
