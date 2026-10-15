package in.fixna.platform.invoiceassist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceSuggestionRepository extends JpaRepository<InvoiceSuggestion, UUID> {

    Optional<InvoiceSuggestion> findByIdAndTenantId(UUID id, UUID tenantId);

    List<InvoiceSuggestion> findByInvoiceIdAndTenantIdOrderByCreatedAtDesc(UUID invoiceId, UUID tenantId);

    boolean existsByInvoiceIdAndTenantIdAndSuggestionTypeAndStatus(
            UUID invoiceId, UUID tenantId, InvoiceSuggestionType suggestionType, InvoiceSuggestionStatus status);
}
