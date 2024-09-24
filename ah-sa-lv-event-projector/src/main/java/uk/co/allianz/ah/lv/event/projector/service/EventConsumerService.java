2package uk.co.allianz.ah.lv.event.projector.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Marker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.audit.MessageStatus;
import uk.co.allianz.ah.lv.event.projector.log.LogContext;
import uk.co.allianz.ah.lv.event.projector.log.LogType;
import uk.co.allianz.ah.lv.event.projector.messaging.event.EventType;
import uk.co.allianz.ah.lv.event.projector.service.processor.ClaimsStpAiResponseEventProcessor;
import uk.co.allianz.ah.lv.event.projector.service.sender.Sender;

import java.util.Map;

import static java.lang.String.format;
import static uk.co.ah.messaging.camel.audit.MessageStatus.IGNORED;
import static uk.co.allianz.ah.lv.event.projector.log.LogContext.mark;
import static uk.co.allianz.ah.lv.event.projector.log.LogContext.markResponseTimeMillis;
import static uk.co.allianz.ah.lv.event.projector.log.LogType.*;
import static uk.co.allianz.ah.lv.event.projector.messaging.event.MessagingHelper.EVENT_TYPE;
import static uk.co.allianz.ah.lv.event.projector.messaging.event.MessagingHelper.ID;

@Slf4j
@Service
public class EventConsumerService {

    public static final int MILLION = 1000000;
    private final Sender onPremSender;

    public EventConsumerService(@Qualifier("onPremSender") final Sender onPremSender) {
        this.onPremSender = onPremSender;
    }

    public MessageStatus consumeEvent(final SaJsonNode data, final Map<String, String> headerMap) {
        final long beginTime = System.nanoTime();
        long elapsedTime;
        try {
            log.info(mark(EVENT_PROJECTION, START), "Starting to handle the event with type {}", headerMap.get(EVENT_TYPE));
            EventType eventType = EventType.find(headerMap.get(EVENT_TYPE));
            LogContext.trackEventType(eventType.value());
            switch (eventType) {
                case AI_RESPONSE:
                    final MessageStatus status = new ClaimsStpAiResponseEventProcessor(onPremSender).handle(data, headerMap);
                    logEventIngestionStatus(eventType, headerMap.get(ID), beginTime, status);
                    return status;
                default:
                    log.info(mark(EVENT_PROJECTION, LogType.IGNORED), "Event ingestion ignored");
                    return IGNORED;
            }
        } catch (Exception e) {
            log.error(mark(EVENT_PROJECTION, LogType.ERROR, markResponseTimeMillis(elapsedTime(beginTime))), format("Event handling for header %s and body %s failed with %s", headerMap, data.prettyPrint(), e.getMessage()), e);
            throw new RuntimeException(format("Event handling for header %s and body %s failed with %s", headerMap, data.prettyPrint(), e.getMessage()), e);
        }
    }

    private void logEventIngestionStatus(final EventType eventType, final String id, final long beginTime, final MessageStatus status) {
        long elapsedTime = elapsedTime(beginTime);

        Marker eventProcessingStatusMarker;
        if (status.equals(MessageStatus.SUCCESS)) {
            eventProcessingStatusMarker = SUCCESS;
        } else {
            eventProcessingStatusMarker = ERROR;
        }

        log.info(mark(EVENT_PROJECTION, eventProcessingStatusMarker, markResponseTimeMillis(elapsedTime)), "Handled {} event for {} in {} ms", eventType, id, elapsedTime);
    }


    private long elapsedTime(final long beginTime) {
        return (System.nanoTime() - beginTime) / MILLION;
    }

}



