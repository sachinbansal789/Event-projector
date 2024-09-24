package uk.co.allianz.ah.lv.event.projector.service.processor;

import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.audit.MessageStatus;

import java.util.Map;

public interface EventProcessor {

    MessageStatus handle(SaJsonNode data, Map<String, String> headerMap);
}
