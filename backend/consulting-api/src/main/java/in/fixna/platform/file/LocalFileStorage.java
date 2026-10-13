package in.fixna.platform.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import in.fixna.platform.common.web.FixnaException;
import in.fixna.platform.identity.auth.JwtProperties;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
@EnableConfigurationProperties({FileStorageProperties.class, JwtProperties.class})
public class LocalFileStorage implements FileStorage {

    private final Path basePath;
    private final SecretKey key;
    private final String baseUrl;

    public LocalFileStorage(FileStorageProperties properties, JwtProperties jwtProperties) {
        this.basePath = Path.of(properties.basePath()).toAbsolutePath().normalize();
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        // Base URL for the API - default to localhost for development
        this.baseUrl = System.getenv().getOrDefault("FIXNA_API_BASE_URL", "http://localhost:8081");
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

    @Override
    public void delete(UUID tenantId, String storageKey) {
        try {
            Path file = basePath.resolve(tenantId.toString()).resolve(storageKey).normalize();
            if (file.startsWith(basePath)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException ex) {
            throw new FixnaException("FILE_DELETE_FAILED", HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete file", ex);
        }
    }

    @Override
    public PreSignedUrl generatePresignedUrl(UUID tenantId, String storageKey, int ttlMinutes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(ttlMinutes));
        String token = Jwts.builder()
                .subject("file_download")
                .claim("tenantId", tenantId.toString())
                .claim("storageKey", storageKey)
                .claim("typ", "file_download")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        String url = baseUrl + "/api/v1/files/download?token=" + token;
        return new PreSignedUrl(url, OffsetDateTime.ofInstant(expiresAt, java.time.ZoneOffset.UTC));
    }

    private static String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "upload";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
