package in.fixna.platform.consulting.proposal.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProposalRequest(
        @NotNull UUID clientId,
        UUID engagementId,
        @NotBlank @Size(max = 255) String title,
        String description,
        @Size(min = 3, max = 3) String currency,
        LocalDate validUntil) {}
