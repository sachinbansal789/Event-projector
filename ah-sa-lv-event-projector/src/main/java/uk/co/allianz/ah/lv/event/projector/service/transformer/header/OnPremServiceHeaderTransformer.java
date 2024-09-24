package uk.co.allianz.ah.lv.event.projector.service.transformer.header;

import java.util.HashMap;
import java.util.Map;

import static uk.co.allianz.ah.lv.event.projector.messaging.event.MessagingHelper.*;

public class OnPremServiceHeaderTransformer implements HeaderTransformer {

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ON_PREMISE_TRANSACTION_ID_HEADER_NAME = "TransactionId";
    public static final String ON_PREMISE_SOURCE_SYSTEM_HEADER_NAME = "SourceSystem";
    public static final String ON_PREMISE_EVENT_TYPE_HEADER_NAME = "EventType";

    private final Map<String, String> headerMap;

    public OnPremServiceHeaderTransformer(final Map<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    @Override
    public Map<String, Object> transform() {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(ON_PREMISE_EVENT_TYPE_HEADER_NAME, headerMap.get(EVENT_TYPE));
        headers.put(CONTENT_TYPE, "application/json");
        headers.put(ON_PREMISE_SOURCE_SYSTEM_HEADER_NAME, headerMap.get(SOURCE));
        headers.put(ON_PREMISE_TRANSACTION_ID_HEADER_NAME, headerMap.get(TRANSACTION_ID));
        //TODO: Check if any extra headers are needed (session id, ref number etc?)
        return headers;
    }
}
