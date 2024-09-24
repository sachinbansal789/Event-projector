package uk.co.allianz.ah.lv.event.projector.service.processor;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.ah.json.dom.SaJsonNode;
import uk.co.ah.messaging.camel.audit.MessageStatus;
import uk.co.allianz.ah.lv.event.projector.service.sender.Sender;
import uk.co.allianz.ah.lv.event.projector.service.transformer.DefaultMessage;
import uk.co.allianz.ah.lv.event.projector.service.transformer.QueueMessage;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static uk.co.allianz.ah.lv.event.projector.messaging.event.EventType.AI_RESPONSE;

@ExtendWith(MockitoExtension.class)
@RunWith(JUnitPlatform.class)
@SpringBootTest(classes = {Sender.class})
class ClaimsStpAiResponseEventProcessorTest {

    @MockBean
    Sender<QueueMessage> onPremSender;

    @InjectMocks
    ClaimsStpAiResponseEventProcessor eventProcessor;

    @Captor
    ArgumentCaptor<DefaultMessage> defaultMessageCapture;

    @Value("classpath:testdata/AIResponse.json")
    Resource aiResponse;

    @Test
    public void canInvokeEventProcessorHandle() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MessageStatus messageStatus = eventProcessor.handle(SaJsonNode.saJsonNode(asString(aiResponse)), constructHeaderMap());
        Mockito.verify(onPremSender).send(defaultMessageCapture.capture());
        Assert.assertEquals(MessageStatus.SUCCESS, messageStatus);
        Assert.assertEquals(AI_RESPONSE,defaultMessageCapture.getValue().type());
        Assert.assertEquals(4,defaultMessageCapture.getValue().headers().size());
    }
    public Map<String, String> constructHeaderMap() {
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("isLogOnly","true");
        headerMap.put("eventType", "someEvent");
        headerMap.put("transactionId","1234");
        headerMap.put("source","PARIS");
        headerMap.put("brandOwner","10");
        headerMap.put("brandType", "7");
        headerMap.put("lob","1");
        headerMap.put("correlld","someId");
        headerMap.put("web-reference","someReference");
        return headerMap;
    }

    private String asString(final Resource resource) throws IOException {
        return StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
    }

}