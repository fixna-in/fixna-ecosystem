package in.fixna.platform.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fixna.file-storage")
public record FileStorageProperties(String basePath) {

    public FileStorageProperties {
        if (basePath == null || basePath.isBlank()) {
            basePath = "./data/files";
        }
    }
}
