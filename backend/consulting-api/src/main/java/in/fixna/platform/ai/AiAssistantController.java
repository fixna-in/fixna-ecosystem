package in.fixna.platform.ai;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.fixna.platform.ai.dto.AiSuggestionResponse;
import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestRequest;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestRequest;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.ai.dto.WebsiteCopySuggestion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "ai", description = "Advisory AI suggestions for consulting workflows")
public class AiAssistantController {

    private final AiAssistantApplicationService aiAssistant;

    public AiAssistantController(AiAssistantApplicationService aiAssistant) {
        this.aiAssistant = aiAssistant;
    }

    @Operation(summary = "Suggest a proposal draft and line items")
    @PostMapping("/proposals/suggest")
    public ResponseEntity<AiSuggestionResponse<ProposalSuggestion>> suggestProposal(
            @Valid @RequestBody ProposalSuggestRequest request) {
        return ResponseEntity.ok(aiAssistant.suggestProposal(request));
    }

    @Operation(summary = "Suggest meeting agenda bullets")
    @PostMapping("/meetings/suggest-agenda")
    public ResponseEntity<AiSuggestionResponse<MeetingAgendaSuggestion>> suggestMeetingAgenda(
            @Valid @RequestBody MeetingAgendaSuggestRequest request) {
        return ResponseEntity.ok(aiAssistant.suggestMeetingAgenda(request));
    }

    @Operation(summary = "Summarize a client with suggested next steps")
    @PostMapping("/clients/{id}/summarize")
    public ResponseEntity<AiSuggestionResponse<ClientSummarySuggestion>> summarizeClient(@PathVariable UUID id) {
        return ResponseEntity.ok(aiAssistant.summarizeClient(id));
    }

    @Operation(summary = "Suggest website tagline and bio from current profile")
    @PostMapping("/website/suggest-copy")
    public ResponseEntity<AiSuggestionResponse<WebsiteCopySuggestion>> suggestWebsiteCopy() {
        return ResponseEntity.ok(aiAssistant.suggestWebsiteCopy());
    }
}
