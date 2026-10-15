package in.fixna.websiteaudit.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Operation(summary = "Liveness probe")
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "fixna-website-audit-api");
    }

    @Operation(summary = "Readiness probe")
    @GetMapping("/ready")
    public Map<String, String> ready() {
        return Map.of("status", "UP", "service", "fixna-website-audit-api");
    }
}
