package in.fixna.platform.ai.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record MeetingAgendaSuggestRequest(@NotBlank String title, UUID clientId) {}
