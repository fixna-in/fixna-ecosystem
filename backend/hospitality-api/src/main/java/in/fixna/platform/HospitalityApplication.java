package in.fixna.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * JWT API — no Spring default user/password (UserDetailsServiceAutoConfiguration disabled).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HospitalityApplication {
    static {
        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl != null && redisUrl.isBlank()) {
            System.setProperty("REDIS_URL", "redis://localhost:6379");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(HospitalityApplication.class, args);
    }
}