package uk.co.allianz.ah.lv.event.projector.configuration;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.ah.encryption.EncryptionHandler;
import uk.co.ah.messaging.camel.CamelScheme;
import uk.co.ah.messaging.camel.CamelSender;


@Configuration
public class CamelSenderConfiguration {

    private final String camelSenderValue;
    private final ProducerTemplate producerTemplate;
    private final EncryptionHandler encryptionHandler;

    public CamelSenderConfiguration(@Value("${camel.scheme}") final String camelScheme, final ProducerTemplate producerTemplate, final EncryptionHandler encryptionHandler) {
        this.camelSenderValue = camelScheme;
        this.producerTemplate = producerTemplate;
        this.encryptionHandler = encryptionHandler;
    }

    @Bean
    CamelSender camelSender() {
        return new CamelSender(new CamelScheme(camelSenderValue), producerTemplate, encryptionHandler);
    }
}
