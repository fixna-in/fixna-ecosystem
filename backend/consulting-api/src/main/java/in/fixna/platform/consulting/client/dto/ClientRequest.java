package in.fixna.platform.consulting.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 320) String email,
        @Size(max = 50) String phone,
        @Size(max = 1000) String websiteUrl,
        @Size(max = 100) String industry,
        String notes) {}
