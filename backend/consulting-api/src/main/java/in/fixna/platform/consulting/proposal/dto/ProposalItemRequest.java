package in.fixna.platform.consulting.proposal.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProposalItemRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        Integer sortOrder) {}
