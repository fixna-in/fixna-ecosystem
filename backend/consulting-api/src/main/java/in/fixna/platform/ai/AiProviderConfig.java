package in.fixna.platform.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderConfig {

    @Bean
    public AiProvider aiProvider(MockAiProvider mockAiProvider) {
        return mockAiProvider;
    }
}
