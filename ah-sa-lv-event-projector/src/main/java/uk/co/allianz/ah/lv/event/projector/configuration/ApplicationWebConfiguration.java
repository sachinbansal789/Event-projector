package uk.co.allianz.ah.lv.event.projector.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.allianz.health.verifiers.DefaultHealthVerifier;
import uk.co.allianz.health.verifiers.HealthVerifier;
import uk.co.allianz.health.verifiers.SpringHealthVerifier;

@Configuration
public class ApplicationWebConfiguration {

    @Value("${health.system.environment}")
    private String currentEnvironment;

    @Bean("healthVerifier")
    public HealthVerifier getSpringSwitchableVerifier() {

        if ("local".equalsIgnoreCase(currentEnvironment)) {
            return new DefaultHealthVerifier();
        }
        return new SpringHealthVerifier();
    }
}
