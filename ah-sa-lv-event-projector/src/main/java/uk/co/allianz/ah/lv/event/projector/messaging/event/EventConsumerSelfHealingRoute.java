package uk.co.allianz.ah.lv.event.projector.messaging.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.routepolicy.quartz.CronScheduledRoutePolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.co.ah.messaging.camel.CamelScheme;
import uk.co.ah.messaging.camel.retry.RetryingHttpRoute;
import uk.co.ah.messaging.camel.security.DecryptionProcessor;
import uk.co.ah.messaging.camel.security.EncryptionProcessor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Optional.ofNullable;
import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.support.builder.PredicateBuilder.and;
import static uk.co.ah.messaging.camel.CamelTracing.TRACING_HEADER_COPIER;
import static uk.co.ah.messaging.camel.CamelTracing.tracer;
import static uk.co.ah.messaging.http.TracingInterceptor.SESSION_ID_HEADER_NAME_FOR_TRACING;

@Slf4j
@Component
public class EventConsumerSelfHealingRoute extends RetryingHttpRoute {

    private static final int MAX_RETRIES = 0;
    public static final int SECONDS = 1;
    private final boolean isSelfHealingEnabled;
    private final String noHealListenerEndpoint;
    private final String healListenerEndpoint;

    private final String healTargetEndpoint;
    private final String noRetryTargetEndpoint;
    private final DecryptionProcessor decryptionProcessor;
    private final EncryptionProcessor encryptionProcessor;
    private static final String ALLIANZ_RETRY_COUNT = "ALLIANZ_RETRY_COUNT";
    private static final int MAX_HEAL_ATTEMPT = 24;
    private static final String LAST_HEALED = "LAST_HEALED";
    private static final String PROCESSING_NEEDED = "PROCESSING_NEEDED";
    private static final String CAN_BE_DELETED = "CAN_BE_DELETED";
    private static final CronScheduledRoutePolicy HEAL_ROUTE_POLICY = new CronScheduledRoutePolicy();
    private static final CronScheduledRoutePolicy NO_HEAL_ROUTE_POLICY = new CronScheduledRoutePolicy();
    public static final int HEAL_ROUTE_START_MINUTE = 0;
    public static final int HEAL_ROUTE_STOP_MINUTE = HEAL_ROUTE_START_MINUTE + 1;
    public static final int NO_HEAL_ROUTE_START_MINUTE = HEAL_ROUTE_STOP_MINUTE + 1;
    public static final int NO_HEAL_ROUTE_STOP_MINUTE = NO_HEAL_ROUTE_START_MINUTE + 2;

    private static final String RETRY_EXCEEDED = "RETRY_EXCEEDED";

    public EventConsumerSelfHealingRoute(final CamelScheme camelScheme, @Value("${ah.selfhealing.enabled}")
    final boolean isSelfHealingEnabled,
                                         @Value("${camel.event.healListener}") final String healListener,
                                         @Value("${camel.event.targetNoHealListener}") final String noHealListener,
                                         @Value("${camel.event.healTarget}") final String healTarget,
                                         @Value("${camel.event.targetNoRetryListener}") final String noRetryTarget,

                                         final DecryptionProcessor decryptionProcessor,
                                         final EncryptionProcessor encryptionProcessor) {
        super(
                "ah-sa-lv-event-projector",
                exchange -> {
                    Throwable exception = exchange.getProperty(EXCEPTION_CAUGHT, Throwable.class);
                    log.info(format("Error processing notification with exception %s", exception));
                },
                MAX_RETRIES,
                Duration.ofSeconds(SECONDS)
        );
        this.isSelfHealingEnabled = isSelfHealingEnabled;
        this.healListenerEndpoint = getEndpoint(camelScheme, healListener);
        this.noHealListenerEndpoint = getEndpoint(camelScheme, noHealListener);
        this.healTargetEndpoint = getEndpoint(camelScheme, healTarget);
        this.noRetryTargetEndpoint = getEndpoint(camelScheme, noRetryTarget);
        this.decryptionProcessor = decryptionProcessor;
        this.encryptionProcessor = encryptionProcessor;

        //healing route to start at second :00, at minute :'HEAL_ROUTE_START_MINUTE', every one hours between 04am and 23pm, of every day
        HEAL_ROUTE_POLICY.setRouteStartTime("0 " + HEAL_ROUTE_START_MINUTE + " 4-23/1 * * ?");
        // healing route to stop after few minutes of route start to have a controlled running time.
        HEAL_ROUTE_POLICY.setRouteStopTime("0 " + HEAL_ROUTE_STOP_MINUTE + " 4-23/1 * * ?");
        // no-heal route to start few minutes after healing route has ended.
        NO_HEAL_ROUTE_POLICY.setRouteStartTime("0 " + NO_HEAL_ROUTE_START_MINUTE + " 4-23/1 * * ?");
        // no-heal route to stop few minutes after to complete the process.
        NO_HEAL_ROUTE_POLICY.setRouteStopTime("0 " + NO_HEAL_ROUTE_STOP_MINUTE + " 4-23/1 * * ?");
    }
    @Override
    public void configure() throws Exception {

        if (isSelfHealingEnabled) {
            super.configure();
            addRouteWithBinding(this.getClass().getSimpleName());
        }
    }

    private String getEndpoint(final CamelScheme camelScheme, final String listener) {
        return camelScheme.getValue() + ":" + listener;
    }

    private void addRouteWithBinding(final String routeId) {
        from(healListenerEndpoint).routeId(routeId + "-healing-route")
                .routePolicy(HEAL_ROUTE_POLICY).noAutoStartup()
                .process(TRACING_HEADER_COPIER)
                .process(tracer())
                .process(decryptionProcessor)
                .process(clearPreviousProperties)
                .process(addRetryCounter)
                .process(maybeMarkForProcessing)
                .process(checkIfRetryExceeded)
                .process(logRetryTryInfo)
                .process(encryptionProcessor)
                .choice()
                    .when(and(isProcessingNeeded, isRetryPossible))
                        .log("Moving message to main queue " + healTargetEndpoint)
                        .process(prepareForRetry)
                        .to(healTargetEndpoint)
                    .otherwise()
                        .log("Moving message to no_heal queue " + noHealListenerEndpoint)
                        .to(noHealListenerEndpoint)
                .end();

        //moving unhealable items back to deadletter queue if retry hasn't exceeded and moving it to non-retry queue if retry has exceeded.
        from(noHealListenerEndpoint).routeId(routeId + "-no-heal-route")
                .routePolicy(NO_HEAL_ROUTE_POLICY).noAutoStartup()
                .process(TRACING_HEADER_COPIER)
                .process(tracer())
                .process(clearPreviousProperties)
                .process(checkIfRetryExceeded)
                .choice()
                    .when(isRetryPossible)
                        .process(exchange -> log.info("moving no-heal messages back to {} queue {}", healListenerEndpoint, Optional.ofNullable(exchange.getIn().getHeader(SESSION_ID_HEADER_NAME_FOR_TRACING, String.class)).orElse("NA")))
                        .to(healListenerEndpoint)
                    .otherwise()
                        .log("Moving message to no retry queue " + noRetryTargetEndpoint)
                        .to(noRetryTargetEndpoint);
    }

    private final Predicate isProcessingNeeded = exchange -> {
        Message in = exchange.getIn();
        return (ofNullable(in.getHeader(PROCESSING_NEEDED)).isPresent() && in.getHeader(PROCESSING_NEEDED).toString().equalsIgnoreCase("true"));
    };

    private final Predicate isRetryPossible = exchange -> {
        Message in = exchange.getIn();
        return (ofNullable(in.getHeader(RETRY_EXCEEDED)).isPresent() && in.getHeader(RETRY_EXCEEDED).toString().equalsIgnoreCase("false"));
    };

    private final Processor clearPreviousProperties = exchange -> {
        exchange.removeProperty(EXCEPTION_CAUGHT);
        exchange.getIn().removeHeader("CamelRabbitmqRoutingKey");
    };

    private final Processor addRetryCounter = exchange -> {
        Message in = exchange.getIn();
        if (ofNullable(in.getHeader(ALLIANZ_RETRY_COUNT)).isEmpty()) {
            in.setHeader(ALLIANZ_RETRY_COUNT, 0);
        }
    };

    private final Processor maybeMarkForProcessing = exchange -> {
        Message in = exchange.getIn();
        Boolean needProcessing = true;
        if (ofNullable(in.getHeader(LAST_HEALED)).isPresent()) {
            final LocalDateTime routeStart = LocalDateTime.now().withMinute(HEAL_ROUTE_START_MINUTE).withSecond(0);
            needProcessing = LocalDateTime.parse(in.getHeader(LAST_HEALED, String.class), ISO_LOCAL_DATE_TIME).isBefore(routeStart);
        }
        in.setHeader(PROCESSING_NEEDED, needProcessing.toString());
    };

    private final Processor checkIfRetryExceeded = exchange -> {
        Message in = exchange.getIn();
        Boolean retryAttemptExceeded = false;
        Boolean lastHealedIsBeforeRouteStart = false;
        if (ofNullable(in.getHeader(ALLIANZ_RETRY_COUNT)).isPresent() && (int) in.getHeader(ALLIANZ_RETRY_COUNT) > MAX_HEAL_ATTEMPT) {
            retryAttemptExceeded = true;
            log.info("retry has exceeded, putting the messages into the non retry queue");
        }
        in.setHeader(RETRY_EXCEEDED, retryAttemptExceeded.toString());
    };

    private final Processor logRetryTryInfo = exchange ->
            log.info("Try: {}, processing {}",
                    getTryCount(exchange.getIn().getHeader(ALLIANZ_RETRY_COUNT, Integer.class)),
                    Optional.ofNullable(exchange.getIn().getHeader(SESSION_ID_HEADER_NAME_FOR_TRACING, String.class)).orElse("NA"));

    private int getTryCount(final Integer lastTryCount) {
        return lastTryCount + 1;
    }

    private final Processor prepareForRetry = exchange -> {
        final Integer thisRetry = (Integer) exchange.getIn().getHeader(ALLIANZ_RETRY_COUNT) + 1;
        exchange.getIn().setHeader(ALLIANZ_RETRY_COUNT, thisRetry);
        exchange.getIn().setHeader(LAST_HEALED, LocalDateTime.now().format(ISO_LOCAL_DATE_TIME));
    };

}
