package uk.co.allianz.ah.lv.event.projector.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import uk.co.ah.messaging.camel.CamelScheme;

import java.time.Duration;

@Configuration
public class ApplicationConfiguration {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private final String camelSchemeValue;

    public ApplicationConfiguration(@Value("${camel.scheme}") final String camelSchemeValue) {
        this.camelSchemeValue = camelSchemeValue;
    }

    @Bean
    CamelScheme camelScheme() {
        return new CamelScheme(camelSchemeValue);
    }

    @Bean
    public RestTemplate restTemplate(final RestTemplateBuilder builder) {
        return builder.setConnectTimeout(TIMEOUT).build();
    }

}

