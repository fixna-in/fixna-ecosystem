package in.fixna.platform.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentReadinessTest {

    @Test
    void flywayMigrationsCoverMvpPhases() throws Exception {
        Path migrationDir = migrationDirectory();
        try (Stream<Path> files = Files.list(migrationDir)) {
            List<String> versions = files
                    .filter(path -> path.getFileName().toString().startsWith("V"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
            assertThat(versions).hasSizeGreaterThanOrEqualTo(9);
            assertThat(versions.getFirst()).startsWith("V1__");
            assertThat(versions.getLast()).startsWith("V9__");
        }
    }

    @Test
    void productionProfileDisablesSwagger() throws Exception {
        Path prodConfig = resourcesDirectory().resolve("application-prod.yml");
        String yaml = Files.readString(prodConfig);
        assertThat(yaml).contains("springdoc:");
        assertThat(yaml).contains("enabled: false");
        assertThat(yaml).contains("hsts-enabled: true");
    }

    @Test
    void dockerfileAndRenderConfigExist() throws Exception {
        Path repoRoot = repoRoot();
        assertThat(Files.exists(repoRoot.resolve("backend/localboost-api/Dockerfile"))).isTrue();
        assertThat(Files.exists(repoRoot.resolve("backend/localboost-api/.dockerignore"))).isTrue();
        assertThat(Files.exists(repoRoot.resolve("render-localboost.yaml"))).isTrue();
        assertThat(Files.exists(repoRoot.resolve("frontend/localboost-web/vercel.json"))).isTrue();
        assertThat(Files.exists(repoRoot.resolve("docs/database/README.md"))).isTrue();
        String render = Files.readString(repoRoot.resolve("render-localboost.yaml"));
        assertThat(render).contains("dockerContext: .");
        assertThat(render).contains("fixna-platform-common");
        assertThat(render).contains("/api/v1/health/readiness");
    }

    @Test
    void ciRunsApiAndWebBuilds() throws Exception {
        String workflow = Files.readString(repoRoot().resolve(".github/workflows/ci.yml"));
        assertThat(workflow).contains("localboost-api:");
        assertThat(workflow).contains("localboost-web:");
        assertThat(workflow).contains("build:localboost");
    }

    @Test
    void databaseScriptsDocumented() {
        Path repoRoot = repoRoot();
        assertThat(Files.exists(repoRoot.resolve("backend/localboost-api/src/main/resources/db/README.md"))).isTrue();
        assertThat(Files.exists(repoRoot.resolve("tools/localboost/sql/schema.sql"))).isTrue();
    }

    private static Path migrationDirectory() {
        Path direct = resourcesDirectory().resolve("db").resolve("migration");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        throw new IllegalStateException("Migration directory not found");
    }

    private static Path resourcesDirectory() {
        Path module = Path.of("").toAbsolutePath();
        Path direct = module.resolve("src").resolve("main").resolve("resources");
        if (Files.isDirectory(direct)) {
            return direct;
        }
        return module.resolve("backend")
                .resolve("localboost-api")
                .resolve("src")
                .resolve("main")
                .resolve("resources");
    }

    private static Path repoRoot() {
        Path module = Path.of("").toAbsolutePath();
        if (Files.exists(module.resolve("backend/localboost-api/pom.xml"))) {
            return module;
        }
        if (Files.exists(module.resolve("pom.xml"))
                && Files.exists(module.resolve("src/main/resources/application-prod.yml"))) {
            return module.getParent().getParent();
        }
        Path parent = module.getParent();
        if (parent != null && Files.exists(parent.resolve("backend/localboost-api/pom.xml"))) {
            return parent;
        }
        return module;
    }
}
