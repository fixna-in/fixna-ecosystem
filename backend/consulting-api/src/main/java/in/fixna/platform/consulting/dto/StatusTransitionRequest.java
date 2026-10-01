package in.fixna.platform.consulting.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusTransitionRequest(@NotBlank String status) {}
