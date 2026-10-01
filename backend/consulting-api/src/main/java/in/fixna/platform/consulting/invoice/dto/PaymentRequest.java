package in.fixna.platform.consulting.invoice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(max = 50) String paymentMethod,
        @Size(max = 255) String reference) {}
