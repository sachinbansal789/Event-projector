package uk.co.allianz.ah.lv.event.projector.messaging.event;

import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.audit.MessageStatus;

public interface EventProcessor {

    MessageStatus process(EventType eventType, SaJsonNode data) throws Exception;

}
