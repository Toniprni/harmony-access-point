package eu.domibus.plugin.webService.backend.dispatch;

import eu.domibus.plugin.webService.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.webService.backend.rules.WSPluginDispatchRulesService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.soap.SOAPMessage;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JMockit.class)
public class WSPluginMessageSenderTest {

    public static final String RULE_NAME = "ruleName";
    public static final String END_POINT = "endpoint";
    @Tested
    private WSPluginMessageSender wsPluginMessageSender;

    @Injectable
    protected WSPluginMessageBuilder wsPluginMessageBuilder;

    @Injectable
    protected WSPluginDispatcher wsPluginDispatcher;

    @Injectable
    protected WSPluginDispatchRulesService wsPluginDispatchRulesService;

    @Test
    public void sendMessageSuccess(@Mocked WSBackendMessageLogEntity wsBackendMessageLogEntity,
                                   @Mocked SOAPMessage soapMessage) {
        new Expectations() {{
            wsPluginMessageBuilder.buildSOAPMessageSendSuccess(wsBackendMessageLogEntity);
            result = soapMessage;
            times = 1;

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsPluginDispatchRulesService.getEndpoint(RULE_NAME);
            result = END_POINT;

            wsPluginDispatcher.dispatch(soapMessage, END_POINT);
            result = soapMessage;
            times = 1;
        }};

        wsPluginMessageSender.sendMessageSuccess(wsBackendMessageLogEntity);

        new FullVerifications() {
        };
    }

    @Test
    public void sendMessageSuccess_exception(
            @Mocked WSBackendMessageLogEntity wsBackendMessageLogEntity,
            @Mocked SOAPMessage soapMessage) {
        new Expectations() {{
            wsPluginMessageBuilder.buildSOAPMessageSendSuccess(wsBackendMessageLogEntity);
            result = soapMessage;
            times = 1;

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsBackendMessageLogEntity.getMessageId();
            result = "MessageId";
            wsPluginDispatchRulesService.getEndpoint(RULE_NAME);
            result = END_POINT;

            wsPluginDispatcher.dispatch(soapMessage, END_POINT);
            result = new IllegalStateException("ERROR");
            times = 1;
        }};

        try {
            wsPluginMessageSender.sendMessageSuccess(wsBackendMessageLogEntity);
            Assert.fail();
        } catch (Exception e) {
            //OK
        }

        new FullVerifications() {
        };
    }
}