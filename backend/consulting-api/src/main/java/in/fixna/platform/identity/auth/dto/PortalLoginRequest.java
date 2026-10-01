package in.fixna.platform.identity.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PortalLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UUID clientId) {

    public String normalizedEmail() {
        return email.trim().toLowerCase();
    }
}
