package in.fixna.platform.file;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void storesAndOpensFile() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(new FileStorageProperties(tempDir.toString()));
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
