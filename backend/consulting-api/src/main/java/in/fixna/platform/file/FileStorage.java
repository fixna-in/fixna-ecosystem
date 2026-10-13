package in.fixna.platform.file;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface FileStorage {

    StoredFile store(UUID tenantId, String originalFilename, InputStream content, long sizeBytes);

    InputStream open(UUID tenantId, String storageKey);

    /** Hard-deletes a stored object; best-effort, safe to call when absent. */
    void delete(UUID tenantId, String storageKey);

    /**
     * Generates a pre-signed URL for downloading a file.
     *
     * @param tenantId the tenant ID
     * @param storageKey the storage key of the file
     * @param ttlMinutes time-to-live in minutes
     * @return a PreSignedUrl containing the URL and expiration time
     */
    PreSignedUrl generatePresignedUrl(UUID tenantId, String storageKey, int ttlMinutes);

    record StoredFile(String storageKey, String originalFilename, long sizeBytes) {}

    record PreSignedUrl(String url, OffsetDateTime expiresAt) {}
}
