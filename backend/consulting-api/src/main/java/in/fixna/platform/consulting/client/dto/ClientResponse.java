package in.fixna.platform.consulting.client.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import in.fixna.platform.consulting.client.Client;
import in.fixna.platform.consulting.client.ClientStatus;

public record ClientResponse(
        UUID id,
        String name,
        String email,
        String phone,
        String websiteUrl,
        String industry,
        String notes,
        ClientStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getWebsiteUrl(),
                client.getIndustry(),
                client.getNotes(),
                client.getStatus(),
                client.getCreatedAt(),
                client.getUpdatedAt());
    }
}
