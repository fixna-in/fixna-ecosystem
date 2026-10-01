package in.fixna.platform.common.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecurityValidatorTest {

    @Test
    void rejectsDevJwtPlaceholder() {
        assertThatThrownBy(() -> ProductionSecurityValidator.validateJwtSecret(
                        ProductionSecurityValidator.DEV_JWT_PLACEHOLDER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FIXNA_JWT_SECRET");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> ProductionSecurityValidator.validateJwtSecret("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void acceptsStrongSecret() {
        assertThatCode(() -> ProductionSecurityValidator.validateJwtSecret(
                        "production-secret-with-enough-entropy-for-hs256!!"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsLocalhostInCorsOrigins() {
        assertThatThrownBy(() -> ProductionSecurityValidator.validateCorsOrigins(
                        "https://consulting.fixna.in,http://localhost:3000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("localhost");
    }

    @Test
    void acceptsProductionCorsOrigins() {
        assertThatCode(() -> ProductionSecurityValidator.validateCorsOrigins("https://consulting.fixna.in"))
                .doesNotThrowAnyException();
    }
}
