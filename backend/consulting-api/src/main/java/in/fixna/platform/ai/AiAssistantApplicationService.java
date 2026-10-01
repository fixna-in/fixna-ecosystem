package in.fixna.platform.ai;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.fixna.platform.audit.AuditEvent;
import in.fixna.platform.audit.AuditPublisher;
import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.ai.dto.AiSuggestionResponse;
import in.fixna.platform.ai.dto.ClientSummarySuggestion;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestRequest;
import in.fixna.platform.ai.dto.MeetingAgendaSuggestion;
import in.fixna.platform.ai.dto.ProposalSuggestRequest;
import in.fixna.platform.ai.dto.ProposalSuggestion;
import in.fixna.platform.ai.dto.WebsiteCopySuggestion;
import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientRepository;
import in.fixna.platform.consulting.profile.ConsultingProfile;
import in.fixna.platform.consulting.profile.ConsultingProfileRepository;
import in.fixna.platform.rbac.Permission;
import in.fixna.platform.tenancy.TenantContext;

@Service
public class AiAssistantApplicationService {

    private final AiProvider aiProvider;
    private final AiUsageLogRepository usageLogs;
    private final ClientRepository clients;
    private final ConsultingProfileRepository profiles;
    private final AuditPublisher audit;

    public AiAssistantApplicationService(
            AiProvider aiProvider,
            AiUsageLogRepository usageLogs,
            ClientRepository clients,
            ConsultingProfileRepository profiles,
            AuditPublisher audit) {
        this.aiProvider = aiProvider;
        this.usageLogs = usageLogs;
        this.clients = clients;
        this.profiles = profiles;
        this.audit = audit;
    }

    @Transactional
    public AiSuggestionResponse<ProposalSuggestion> suggestProposal(ProposalSuggestRequest request) {
        requireAiPermission();
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Client client = requireClient(request.clientId());

        ProposalSuggestion suggestion = aiProvider.suggestProposalDraft(client, request.topic());
        logUsage(tenantId, userId, AiFeature.PROPOSAL_DRAFT, "clientId=" + request.clientId());
        publishAudit(tenantId, userId, AiFeature.PROPOSAL_DRAFT, request.clientId().toString());
        return AiSuggestionResponse.of(suggestion);
    }

    @Transactional
    public AiSuggestionResponse<MeetingAgendaSuggestion> suggestMeetingAgenda(MeetingAgendaSuggestRequest request) {
        requireAiPermission();
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Client client = request.clientId() != null ? requireClient(request.clientId()) : null;

        MeetingAgendaSuggestion suggestion = aiProvider.suggestMeetingAgenda(request.title(), client);
        String summary = "title=" + request.title()
                + (request.clientId() != null ? ", clientId=" + request.clientId() : "");
        logUsage(tenantId, userId, AiFeature.MEETING_AGENDA, summary);
        publishAudit(tenantId, userId, AiFeature.MEETING_AGENDA, request.title());
        return AiSuggestionResponse.of(suggestion);
    }

    @Transactional
    public AiSuggestionResponse<ClientSummarySuggestion> summarizeClient(UUID clientId) {
        requireAiPermission();
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Client client = requireClient(clientId);

        ClientSummarySuggestion suggestion = aiProvider.suggestClientSummary(client);
        logUsage(tenantId, userId, AiFeature.CLIENT_SUMMARY, "clientId=" + clientId);
        publishAudit(tenantId, userId, AiFeature.CLIENT_SUMMARY, clientId.toString());
        return AiSuggestionResponse.of(suggestion);
    }

    @Transactional
    public AiSuggestionResponse<WebsiteCopySuggestion> suggestWebsiteCopy() {
        requireAiPermission();
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        ConsultingProfile profile = profiles.findByTenantId(tenantId).orElseGet(() -> {
            ConsultingProfile empty = new ConsultingProfile();
            empty.setTenantId(tenantId);
            return empty;
        });

        WebsiteCopySuggestion suggestion = aiProvider.suggestWebsiteCopy(profile);
        logUsage(tenantId, userId, AiFeature.WEBSITE_COPY, "tenantId=" + tenantId);
        publishAudit(tenantId, userId, AiFeature.WEBSITE_COPY, tenantId.toString());
        return AiSuggestionResponse.of(suggestion);
    }

    private void requireAiPermission() {
        TenantContext.requirePermission(Permission.AI_USE);
    }

    private Client requireClient(UUID clientId) {
        UUID tenantId = TenantContext.requireTenantId();
        return clients.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "Client not found"));
    }

    private void logUsage(UUID tenantId, UUID userId, AiFeature feature, String requestSummary) {
        AiUsageLog log = new AiUsageLog();
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setFeature(feature);
        log.setRequestSummary(requestSummary);
        usageLogs.save(log);
    }

    private void publishAudit(UUID tenantId, UUID userId, AiFeature feature, String entityId) {
        audit.publish(new AuditEvent(
                "ai.suggestion.generated",
                tenantId,
                userId,
                "ai_suggestion",
                entityId,
                Map.of("feature", feature.name()),
                null));
    }
}
