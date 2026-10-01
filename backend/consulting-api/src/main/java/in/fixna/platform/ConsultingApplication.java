package in.fixna.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ConsultingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultingApplication.class, args);
    }
}
