package uk.co.allianz.ah.lv.event.projector.service.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.co.ah.messaging.camel.CamelSender;
import uk.co.ah.security.service.ServiceTokenProvider;
import uk.co.allianz.ah.lv.event.projector.log.LogContext;
import uk.co.allianz.ah.lv.event.projector.log.LogType;
import uk.co.allianz.ah.lv.event.projector.service.transformer.QueueMessage;

import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.co.ah.messaging.http.TracingInterceptor.APPLICATION_SOURCE;
import static uk.co.allianz.ah.lv.event.projector.log.LogContext.markResponseTimeMillis;

public class OnPremSender implements Sender<QueueMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnPremSender.class);
    private final String url;
    private final CamelSender sender;
    private final ServiceTokenProvider serviceTokenProvider;

    public OnPremSender(final String url, final CamelSender sender, final ServiceTokenProvider serviceTokenProvider) {
        this.url = url;
        this.sender = sender;
        this.serviceTokenProvider = serviceTokenProvider;
    }

    @Override
    public void send(final QueueMessage message) {
        final long beginTime = System.nanoTime();
        if (isPerformanceTest()) {
            LOGGER.info(LogContext.mark(LogType.ONPREM_EVENT_RECEIVER_API_CALL, LogType.IGNORED), "Not really sending to On Prem Event Receiver, since the event is from a performance test");
        } else {
            try {
                LOGGER.info(LogContext.mark(LogType.ONPREM_EVENT_RECEIVER_API_CALL, LogType.START), "Sending event to On Prem Event Receiver");
                sender.sendBodyAndHeaders(url, message.payload().toString(), setAuthorization(message.headers()), true);
                final long elapsedTime = (System.nanoTime() - beginTime) / 1000000;
                LOGGER.info(LogContext.mark(LogType.ONPREM_EVENT_RECEIVER_API_CALL, LogType.SUCCESS, markResponseTimeMillis(elapsedTime)), "Successfully sent event to On Prem Event Receiver");
            } catch (Exception e) {
                final long elapsedTime = (System.nanoTime() - beginTime) / 1000000;
                LOGGER.error(LogContext.mark(LogType.ONPREM_EVENT_RECEIVER_API_CALL, LogType.ERROR, markResponseTimeMillis(elapsedTime)), "Exception while sending event to On Prem Event Receiver message ", e);
                throw new RuntimeException("Exception while sending event to On Prem Event Receiver message ", e);
            }
        }
    }

    private Map<String, Object> setAuthorization(final Map<String, Object> headers) {
        headers.put(AUTHORIZATION, "Bearer " + getAuthToken());
        return headers;
    }

    private boolean isPerformanceTest() {
        return ofNullable(MDC.get(APPLICATION_SOURCE)).map(s -> s.equalsIgnoreCase("performance-test")).orElse(false);
    }

    private String getAuthToken() {
        try {
            LOGGER.info("Invoking OAuth token service for On Prem Event Receiver apigee proxy...");
            return serviceTokenProvider.getAuthToken();
        } catch (Exception e) {
            LOGGER.error("Exception while invoking OAuth Proxy from LV Event Projector for On Prem Event Receiver apigee proxy", e);
            throw new RuntimeException("Exception while invoking OAuth Proxy from LV Event Projector for On Prem Event Receiver apigee proxy", e);
        }
    }
}
