package in.fixna.platform.common.observability;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadinessControllerTest {

    private static final String APP = "fixna-consulting-api";

    @Test
    void returnsReadyWhenRunnable() {
        ReadinessController controller = new ReadinessController(() -> true, APP);

        var response = controller.ready();
        Map<String, Object> body = response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("READY");
        assertThat(body.get("service")).isEqualTo(APP);
    }

    @Test
    void returns503UntilContextIsReady() {
        ReadinessController controller = new ReadinessController(() -> false, APP);

        var response = controller.ready();
        Map<String, Object> body = response.getBody();

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("DOWN");
        assertThat(body.get("service")).isEqualTo(APP);
    }
}
