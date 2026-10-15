package in.fixna.platform.file;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import in.fixna.platform.identity.auth.JwtProperties;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void storesAndOpensFile() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(
                new FileStorageProperties(tempDir.toString()),
                new JwtProperties("test-secret-32bytes-minimum-length!!", Duration.ofMinutes(15), Duration.ofDays(7)));
        UUID tenantId = UUID.randomUUID();
        byte[] content = "hello".getBytes();

        FileStorage.StoredFile stored = storage.store(
                tenantId, "note.txt", new ByteArrayInputStream(content), content.length);

        assertThat(stored.storageKey()).isNotBlank();
        assertThat(Files.exists(tempDir.resolve(tenantId.toString()).resolve(stored.storageKey()))).isTrue();

        try (var in = storage.open(tenantId, stored.storageKey())) {
            assertThat(in.readAllBytes()).isEqualTo(content);
        }
    }
}
