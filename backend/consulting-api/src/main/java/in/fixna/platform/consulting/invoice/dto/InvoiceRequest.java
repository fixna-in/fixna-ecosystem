package in.fixna.platform.consulting.invoice.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InvoiceRequest(
        @NotNull UUID clientId,
        UUID engagementId,
        UUID proposalId,
        @NotBlank @Size(max = 255) String title,
        @Size(min = 3, max = 3) String currency,
        LocalDate dueDate) {}
