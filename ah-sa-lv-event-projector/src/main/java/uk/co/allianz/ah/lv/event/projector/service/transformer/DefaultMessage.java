package uk.co.allianz.ah.lv.event.projector.service.transformer;


import uk.co.allianz.ah.lv.event.projector.messaging.event.EventType;

import java.util.Map;

public class DefaultMessage implements QueueMessage<String> {

    private final EventType eventType;
    private final Map<String, Object> headers;
    private final String payload;

    public DefaultMessage(final EventType eventType, final Map<String, Object> headers, final String payload) {
        this.eventType = eventType;
        this.headers = headers;
        this.payload = payload;
    }

    public DefaultMessage(final EventType eventType, final String payload) {
        this.eventType = eventType;
        this.headers = null;
        this.payload = payload;
    }

    @Override
    public Map<String, Object> headers() {
        return headers;
    }

    @Override
    public String payload() {
        return payload;
    }

    @Override
    public EventType type() {
        return eventType;
    }
}
