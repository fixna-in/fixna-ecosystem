package in.fixna.platform.ai.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ProposalSuggestRequest(@NotNull UUID clientId, String topic) {}
