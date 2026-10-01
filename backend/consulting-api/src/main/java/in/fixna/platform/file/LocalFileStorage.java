package in.fixna.platform.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;

@Component
@EnableConfigurationProperties(FileStorageProperties.class)
public class LocalFileStorage implements FileStorage {

    private final Path basePath;

    public LocalFileStorage(FileStorageProperties properties) {
        this.basePath = Path.of(properties.basePath()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(UUID tenantId, String originalFilename, InputStream content, long sizeBytes) {
        try {
            Path tenantDir = basePath.resolve(tenantId.toString());
            Files.createDirectories(tenantDir);
            String storageKey = UUID.randomUUID() + "-" + sanitize(originalFilename);
            Path target = tenantDir.resolve(storageKey);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(storageKey, originalFilename, sizeBytes);
        } catch (IOException ex) {
            throw new FixnaException("FILE_STORE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store file", ex);
        }
    }

    @Override
    public InputStream open(UUID tenantId, String storageKey) {
        try {
            Path file = basePath.resolve(tenantId.toString()).resolve(storageKey).normalize();
            if (!file.startsWith(basePath)) {
                throw new FixnaException("FORBIDDEN", HttpStatus.FORBIDDEN, "Invalid storage key");
            }
            if (!Files.exists(file)) {
                throw new FixnaException("NOT_FOUND", HttpStatus.NOT_FOUND, "File not found");
            }
            return Files.newInputStream(file);
        } catch (IOException ex) {
            throw new FixnaException("FILE_OPEN_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Unable to open file", ex);
        }
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
