package uk.co.allianz.ah.lv.event.projector.messaging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.model.ProcessorDefinition;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.CamelScheme;
import uk.co.ah.messaging.camel.retry.RetryingHttpRoute;
import uk.co.ah.messaging.camel.security.DecryptionProcessor;
import uk.co.allianz.ah.lv.event.projector.service.EventConsumerService;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static uk.co.ah.json.dom.SaJsonNode.saJsonNode;
import static uk.co.ah.messaging.camel.CamelTracing.*;
import static uk.co.ah.messaging.camel.Constants.EVENT_TYPE_HEADER_NAME;
import static uk.co.ah.messaging.camel.Constants.NOTIFICATION_TYPE_HEADER_NAME;
import static uk.co.ah.messaging.http.TracingInterceptor.APPLICATION_SOURCE;
import static uk.co.ah.messaging.http.TracingInterceptor.SESSION_ID_HEADER_NAME_FOR_TRACING;
import static uk.co.allianz.ah.lv.event.projector.log.LogContext.mark;
import static uk.co.allianz.ah.lv.event.projector.log.LogType.*;
import static uk.co.allianz.ah.lv.event.projector.messaging.event.MessagingHelper.*;

@Slf4j
@Component
public class EventConsumerRoute extends RetryingHttpRoute {
    public static final String HANDLE_STATUS = "handleStatus";
    public static final int RETRY_DELAY_SECONDS = 2;
    public static final int SECONDS = 3;
    private static final int MAX_RETRIES = 1;
    private final String listenerEndpoint;
    private final String retryListenerEndpoint;
    private final DecryptionProcessor decryptionProcessor;
    private final EventConsumerService eventConsumerService;

    public EventConsumerRoute(final EventConsumerService eventConsumerService,
                               final CamelScheme camelScheme,
                               @Value("${camel.event.listener}") final String listener,
                               @Value("${camel.event.retryListener}") final String retryListener,
                               @Value("${camel.event.deadletter}") final String deadletter,
                               final DecryptionProcessor decryptionProcessor) {
        super(
                "ah-sa-lv-event-projector",
                exchange -> {
                    tracingHeaderCopier(exchange);
                    Throwable exception = exchange.getProperty(EXCEPTION_CAUGHT, Throwable.class);
                    log.error(format("Error processing event with exception %s for %s", exception, eventType(exchange)));
                },
                MAX_RETRIES,
                Duration.ofSeconds(SECONDS)
        );

        this.eventConsumerService = eventConsumerService;
        this.decryptionProcessor = decryptionProcessor;
        this.listenerEndpoint = getEndpoint(camelScheme, listener, deadletter);
        this.retryListenerEndpoint = getEndpoint(camelScheme, retryListener, deadletter);
    }

    @Override
    public void configure() throws Exception {
        super.configure();
        addRouteWithBinding(listenerEndpoint, this.getClass().getSimpleName());
        addRouteWithBinding(retryListenerEndpoint, "routeToAddRetryBindingToExchange");
    }

    private ProcessorDefinition<?> addRouteWithBinding(final String endpoint, final String routeId) {
        return fromF(endpoint).routeId(routeId)
                .process(this::clearPreviousProperties)
                .process(TRACING_HEADER_COPIER)
                .process(tracer())
                .process(decryptionProcessor)
                .process(exchange -> {
                    final SaJsonNode jsonPayload = jsonBody(exchange);
                    final MessagingHelper messagingHelper = new MessagingHelper();
                    final Map<String, String> headerMap = messagingHelper.getEventHeaderMap(jsonPayload.toString(), String.valueOf(exchange.getIn().getHeader(ID, "")));
                    messagingHelper.addTracingHeaders(headerMap);
                    log.info(mark(EVENT_RECEPTION, SUCCESS), "Event received and ready to process");
                    String status = eventConsumerService.consumeEvent(jsonPayload, headerMap).toString();
                    exchange.setProperty(HANDLE_STATUS, status);
                })
                .process(exchange -> log.info("Handled event of type {} as {}", eventType(exchange), getHandledStatus(exchange))).end();
    }

    private static SaJsonNode jsonBody(final Exchange exchange) {
        return saJsonNode(exchange.getIn().getBody(String.class));
    }

    private static EventType eventType(final Exchange exchange) {
        if (exchange.getIn().getHeaders().containsKey(NOTIFICATION_TYPE_HEADER_NAME)) {
            return EventType.find(exchange.getIn().getHeader(NOTIFICATION_TYPE_HEADER_NAME, String.class));
        } else {
            return EventType.find(exchange.getIn().getHeader(EVENT_TYPE_HEADER_NAME, String.class));
        }
    }

    private String getEndpoint(final CamelScheme camelScheme, final String listener, final String deadletter) {
        final String value = camelScheme.getValue() + ":" + listener;
        if (deadletter.isEmpty()) {
            return value;
        }
        return value + "&" + deadletter;
    }

    private Object getHandledStatus(final Exchange exchange) {
        return Optional.ofNullable(exchange.getProperty(HANDLE_STATUS)).orElse("Unknown");
    }

    private void clearPreviousProperties(final Exchange exchange) {
        exchange.removeProperty(EXCEPTION_CAUGHT);
        exchange.removeProperty(HANDLE_STATUS);
        MDC.remove(TRANSACTION_ID);
        MDC.remove(EVENT_TYPE);
        MDC.remove(EVENT_SUBTYPE);
        MDC.remove(SESSION_ID_HEADER_NAME_FOR_TRACING);
        MDC.remove(APPLICATION_SOURCE);
    }
}
