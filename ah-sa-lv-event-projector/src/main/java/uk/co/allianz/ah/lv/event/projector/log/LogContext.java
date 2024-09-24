package uk.co.allianz.ah.lv.event.projector.log;

import net.logstash.logback.marker.Markers;
import org.slf4j.MDC;
import org.slf4j.Marker;

public final class LogContext {
    public static final String EVENT_TYPE = "eventType";
    public static final String JOURNEY_TYPE = "journey_type";
    public static final String ACTION_TYPE = "action_type";
    public static final String ACTION_STATUS = "action_status";
    public static final String RESPONSE_TIME_MILLIS = "response_time_millis";
    public static final String REFERENCE_NUMBER = "reference_number";
    public static final String TRANSACTION_ID = "transactionId";

    private LogContext() {
    }

    public static void trackEventType(final String eventType) {
        track(EVENT_TYPE, eventType);
    }

    public static void trackJourney(final String journeyType) {
        track(JOURNEY_TYPE, journeyType);
    }

    public static void trackReferenceNumber(final String referenceNumber) {
        track(REFERENCE_NUMBER, referenceNumber);
    }

    public static void trackTransactionId(final String transactionId) {
        track(TRANSACTION_ID, transactionId);
    }

    public static Marker markAction(final String actionType) {
        return Markers.append(ACTION_TYPE, actionType);
    }

    public static Marker markActionStatus(final String actionStatus) {
        return Markers.append(ACTION_STATUS, actionStatus);
    }

    public static Marker markResponseTimeMillis(final long responseTimeMillis) {
        return Markers.append(RESPONSE_TIME_MILLIS, responseTimeMillis);
    }

    public static Marker markReferenceNumber(final String referenceNumber) {
        return Markers.append(REFERENCE_NUMBER, referenceNumber);
    }

    public static Marker markEventType(final String eventType) {
        return Markers.append(EVENT_TYPE, eventType);
    }

    public static Marker mark(final String name, final String value) {
        return Markers.append(name, value);
    }

    public static Marker mark(final Marker... markers) {
        Marker toReturn = Markers.empty().and(markers[0]);

        for (int i = 1; i < markers.length; ++i) {
            toReturn.add(markers[i]);
        }

        return toReturn;
    }

    public static void track(final String name, final String value) {
        MDC.put(name, value);
    }

    public static void clearTracking(final String... trackingKeys) {
        if (trackingKeys.length == 0) {
            MDC.clear();
        }

        for (String trackingKey : trackingKeys) {
            MDC.remove(trackingKey);
        }
    }
}
