package in.fixna.platform.common.observability;

import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/health")
public class ReadinessController {

    private final RunnableIsReady isReady;
    private final String appName;

    public ReadinessController(
            RunnableIsReady isReady,
            @Value("${spring.application.name:fixna-consulting-api}") String appName) {
        this.isReady = isReady;
        this.appName = appName;
    }

    @Operation(summary = "Readiness probe — 200 when the app is ready to serve traffic")
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        if (isReady.get()) {
            return ResponseEntity.ok(Map.of(
                    "status", "READY",
                    "service", appName,
                    "timestamp", OffsetDateTime.now().toString()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DOWN",
                "service", appName,
                "timestamp", OffsetDateTime.now().toString()));
    }

    public interface RunnableIsReady {
        boolean get();
    }
}
