package in.fixna.platform.identity.auth;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.identity.auth.dto.AuthResponse;
import in.fixna.platform.identity.auth.dto.PortalLoginRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/portal/auth")
@Tag(name = "portal-auth", description = "Client portal login")
public class PortalAuthController {

    private final AuthService authService;

    public PortalAuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login to client portal with email, password, and client ID")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody PortalLoginRequest request) {
        return ResponseEntity.ok(authService.portalLogin(request));
    }
}
