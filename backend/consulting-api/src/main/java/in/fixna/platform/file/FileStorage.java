package in.fixna.platform.file;

import java.io.InputStream;
import java.util.UUID;

public interface FileStorage {

    StoredFile store(UUID tenantId, String originalFilename, InputStream content, long sizeBytes);

    InputStream open(UUID tenantId, String storageKey);

    record StoredFile(String storageKey, String originalFilename, long sizeBytes) {}
}
