package uk.co.allianz.ah.lv.event.projector.service.processor;

import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.audit.MessageStatus;
import uk.co.allianz.ah.lv.event.projector.log.LogContext;
import uk.co.allianz.ah.lv.event.projector.service.sender.Sender;
import uk.co.allianz.ah.lv.event.projector.service.transformer.DefaultMessage;
import uk.co.allianz.ah.lv.event.projector.service.transformer.QueueMessage;
import uk.co.allianz.ah.lv.event.projector.service.transformer.header.OnPremServiceHeaderTransformer;

import java.util.Map;

import static uk.co.allianz.ah.lv.event.projector.messaging.event.EventType.AI_RESPONSE;

public class ClaimsStpAiResponseEventProcessor implements EventProcessor {

    private Sender<QueueMessage> onPremSender;

    private static final String CLAIM_NUMBER = "claimNumber";
    private static final String TRANSACTION_ID = "transactionId";

    public ClaimsStpAiResponseEventProcessor(final Sender<QueueMessage> onPremSender) {
        this.onPremSender = onPremSender;
    }

    @Override
    public MessageStatus handle(final SaJsonNode aiResponseEvent, final Map<String, String> headerMap) {
        String claimNumberFromEvent = getClaimNumberFromEvent(aiResponseEvent);
        String transactionId = getTransactionId(headerMap);

        LogContext.trackReferenceNumber(claimNumberFromEvent);
        LogContext.trackTransactionId(transactionId);

        Map<String, Object> headers = new OnPremServiceHeaderTransformer(headerMap).transform();
        onPremSender.send(new DefaultMessage(AI_RESPONSE, headers, aiResponseEvent.prettyPrint()));
        return MessageStatus.SUCCESS;
    }

    private String getClaimNumberFromEvent(final SaJsonNode aiResponseEvent) {
        return aiResponseEvent.nestedGet("event_details.ai_response.json_message.control").string(CLAIM_NUMBER);
    }

    protected String getTransactionId(final Map<String, String> headerMap) {
        return headerMap.get(TRANSACTION_ID);
    }

}
