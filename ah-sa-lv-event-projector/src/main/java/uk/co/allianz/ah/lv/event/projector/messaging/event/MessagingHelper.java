package uk.co.allianz.ah.lv.event.projector.messaging.event;


import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import uk.co.ah.json.dom.SaJsonNode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static uk.co.ah.json.dom.SaJsonNode.saJsonNode;
import static uk.co.ah.messaging.camel.Constants.NOTIFICATION_TYPE_HEADER_NAME;
import static uk.co.ah.messaging.http.TracingInterceptor.*;

public final class MessagingHelper {

    public static final String TRANSACTION_ID = "transactionId";
    public static final String TRANSACTION_EFF_DATE = "transactionEffectiveDate";
    public static final String EVENT_TYPE = "eventType";
    public static final String EVENT_SUBTYPE = "eventSubType";
    public static final String ID = "id";
    public static final String SOURCE = "source";

    public MessagingHelper() {
    }

    public Map<String, String> getEventHeaderMap(final String crmMsg, final String id) {
        Map<String, String> headerMap = new HashMap<>();
        SaJsonNode node = saJsonNode(crmMsg);
        headerMap.put(EVENT_TYPE, node.path("event_control").string("event_type"));
        headerMap.put(TRANSACTION_ID, node.path("event_control").string("transaction_id"));
        headerMap.put(EVENT_SUBTYPE, node.path("event_control").string("event_sub_type"));
        headerMap.put(SOURCE, node.path("event_control").string("transaction_source"));
        headerMap.put(TRANSACTION_EFF_DATE, node.path("event_control").string("transaction_effective_date"));
        if (StringUtils.isBlank(id)) {
            headerMap.put(ID, UUID.randomUUID().toString());
        } else {
            headerMap.put(ID, id);
        }
        return headerMap;
    }

    public String enrichPayloadWithEventControl(final String payload, final Exchange exchange) {
        SaJsonNode node = withUnwantedElementsRemoved(saJsonNode(payload));
        final String eventType = exchange.getIn().getHeader(NOTIFICATION_TYPE_HEADER_NAME, "TYPE_NOT_FOUND", String.class);
        node.post("event_control", saJsonNode()
                .post("event_type", eventType)
                .post("transaction_id", generateTransactionIdFromPayload(node))
                .post("event_sub_type", "")
                .post("transaction_source", generateWebApplicationSource(node))
                .post("transaction_effective_date", node.path("officeUseOnly").string("lastModifiedDate"))
        );
        return node.prettyPrint();
    }

    private String generateWebApplicationSource(final SaJsonNode node) {
        return "WEB_" + node.nestedGet("officeUseOnly.params").string("channel").toUpperCase(Locale.ENGLISH);
    }

    private String generateTransactionIdFromPayload(final SaJsonNode node) {
        long epochTime = LocalDateTime.parse(node.path("officeUseOnly").string("lastModifiedDate"), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")).toEpochSecond(ZoneOffset.UTC);
        return node.path("officeUseOnly").string("quoteReference") + epochTime;
    }

    private SaJsonNode withUnwantedElementsRemoved(final SaJsonNode node) {
        if (node.has("runningTotals")) {
            return node.remove("runningTotals");
        }
        return node;
    }

    public void addTracingHeaders(final Map<String, String> headerMap) {
        MDC.put(TRANSACTION_ID, headerMap.get(TRANSACTION_ID));
        MDC.put(EVENT_TYPE, headerMap.get(EVENT_TYPE));
        MDC.put(EVENT_SUBTYPE, headerMap.get(EVENT_SUBTYPE));
        MDC.put(SESSION_ID_HEADER_NAME_FOR_TRACING, headerMap.get(ID));
        MDC.put(APPLICATION_SOURCE, headerMap.get(SOURCE));
    }

    public void addRetryTracingHeaders(final Map<String, String> headerMap, final String sourceDetails) {
        MDC.put(EVENT_TYPE, headerMap.get(EVENT_TYPE));
        MDC.put(EVENT_SUBTYPE, headerMap.get(EVENT_SUBTYPE));
        MDC.put(CORRELATION_ID_HEADER_NAME_FOR_TRACING, UUID.randomUUID().toString());
        MDC.put(SESSION_ID_HEADER_NAME_FOR_TRACING, headerMap.get(ID));
        MDC.put(APPLICATION_SOURCE, headerMap.get(SOURCE));
        MDC.put(APPLICATION_SOURCE_DETAILS, sourceDetails);
    }
}
