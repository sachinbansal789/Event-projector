package uk.co.allianz.ah.lv.event.projector.service.transformer;


import uk.co.allianz.ah.lv.event.projector.messaging.event.EventType;

import java.util.Map;

public interface QueueMessage<K> extends Message<K> {

    Map<String, Object> headers();

    EventType type();
}
