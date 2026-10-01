package in.fixna.platform.consulting.invoice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InvoiceLineRequest(
        @NotBlank String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        Integer sortOrder) {}
