package uk.co.allianz.ah.lv.event.projector.configuration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.co.ah.messaging.camel.CamelSender;
import uk.co.ah.security.service.ServiceTokenProvider;
import uk.co.allianz.ah.lv.event.projector.service.sender.OnPremSender;
import uk.co.allianz.ah.lv.event.projector.service.sender.Sender;
import uk.co.allianz.ah.lv.event.projector.service.transformer.QueueMessage;

@Configuration
public class ApiSenderConfiguration {

    private final String apigeeOnPremIngressUrl;
    private final CamelSender camelSender;
    private final ServiceTokenProvider serviceTokenProvider;

    public ApiSenderConfiguration(@Value("${ingress.url}") final String apigeeOnPremIngressUrl,
                                  final ServiceTokenProvider serviceTokenProvider,
                                  final CamelSender camelSender) {
        this.apigeeOnPremIngressUrl = apigeeOnPremIngressUrl;
        this.serviceTokenProvider = serviceTokenProvider;
        this.camelSender = camelSender;
    }

    @Bean
    @Qualifier("onPremSender")
    Sender<QueueMessage> onPremSender() {
        return new OnPremSender(apigeeOnPremIngressUrl, camelSender, serviceTokenProvider);
    }

}

