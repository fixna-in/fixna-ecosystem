package in.fixna.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JWT API — no Spring default user/password (UserDetailsServiceAutoConfiguration disabled).
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaRepositories(basePackages = "in.fixna.platform")
@EntityScan(basePackages = "in.fixna.platform")
@EnableTransactionManagement
public class FixnaApplication {
    public static void main(String[] args) {
        SpringApplication.run(FixnaApplication.class, args);
    }
}
