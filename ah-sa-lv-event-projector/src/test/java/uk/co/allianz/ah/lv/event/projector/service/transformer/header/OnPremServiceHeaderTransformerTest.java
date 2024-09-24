package uk.co.allianz.ah.lv.event.projector.service.transformer.header;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.co.allianz.ah.lv.event.projector.messaging.event.MessagingHelper.*;
import static uk.co.allianz.ah.lv.event.projector.service.transformer.header.OnPremServiceHeaderTransformer.*;

public class OnPremServiceHeaderTransformerTest {

    @Test
    public void testOnPremiseHeadersTransformer() {
        Map<String, String> inputHeaderMap = new HashMap<>();
        inputHeaderMap.put(EVENT_TYPE, "anEventType");
        inputHeaderMap.put(TRANSACTION_ID, "aTransactionId");
        inputHeaderMap.put(EVENT_SUBTYPE, "aEventSubtype");
        inputHeaderMap.put(SOURCE, "aSource");
        inputHeaderMap.put(TRANSACTION_EFF_DATE, "aDate");

        Map<String, Object> transformedHeaderMap = new OnPremServiceHeaderTransformer(inputHeaderMap).transform();

        assertEquals(4, transformedHeaderMap.size());
        assertTrue(transformedHeaderMap.containsKey(ON_PREMISE_EVENT_TYPE_HEADER_NAME));
        assertTrue(transformedHeaderMap.containsKey(CONTENT_TYPE));
        assertTrue(transformedHeaderMap.containsKey(ON_PREMISE_SOURCE_SYSTEM_HEADER_NAME));
        assertTrue(transformedHeaderMap.containsKey(ON_PREMISE_TRANSACTION_ID_HEADER_NAME));

        assertEquals("anEventType", transformedHeaderMap.get(ON_PREMISE_EVENT_TYPE_HEADER_NAME));
        assertEquals("application/json", transformedHeaderMap.get(CONTENT_TYPE));
        assertEquals("aSource", transformedHeaderMap.get(ON_PREMISE_SOURCE_SYSTEM_HEADER_NAME));
        assertEquals("aTransactionId", transformedHeaderMap.get(ON_PREMISE_TRANSACTION_ID_HEADER_NAME));
    }

}