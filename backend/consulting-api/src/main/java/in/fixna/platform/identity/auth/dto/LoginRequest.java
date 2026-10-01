package in.fixna.platform.identity.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {

    public String normalizedEmail() {
        return email == null ? null : email.trim().toLowerCase();
    }
}
