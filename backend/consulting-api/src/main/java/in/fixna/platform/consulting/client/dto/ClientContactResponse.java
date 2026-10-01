package in.fixna.platform.consulting.client.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.client.ClientContact;

public record ClientContactResponse(
        UUID id,
        UUID clientId,
        String name,
        String email,
        String phone,
        String jobTitle,
        boolean primaryContact,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ClientContactResponse from(ClientContact contact) {
        return new ClientContactResponse(
                contact.getId(),
                contact.getClientId(),
                contact.getName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getJobTitle(),
                contact.isPrimaryContact(),
                contact.getCreatedAt(),
                contact.getUpdatedAt());
    }
}
