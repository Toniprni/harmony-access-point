package eu.domibus.plugin.ws.backend.dispatch;

import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageStatus;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.backend.reliability.WSPluginBackendReliabilityService;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRule;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRulesService;
import eu.domibus.plugin.ws.backend.*;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.exception.WSPluginException;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@RunWith(JMockit.class)
public class WSPluginMessageSenderTest {

    public static final String RULE_NAME = "ruleName";
    public static final String END_POINT = "endpoint";
    public static final Long ID = 1L;
    public static final String MESSAGE_ID = "MessageId";
    @Tested
    private WSPluginMessageSender wsPluginMessageSender;

    @Injectable
    protected WSPluginMessageBuilder wsPluginMessageBuilder;

    @Injectable
    protected WSPluginDispatcher wsPluginDispatcher;

    @Injectable
    protected WSPluginDispatchRulesService wsPluginDispatchRulesService;

    @Injectable
    protected WSPluginBackendReliabilityService reliabilityService;

    @Injectable
    protected WSPluginImpl wsPlugin;

    @Injectable
    protected XMLUtil xmlUtil;

    @Injectable
    protected WSBackendMessageLogDao wsBackendMessageLogDao;

    @Test(expected = WSPluginException.class)
    public void sendSubmitMessage_noRule(@Injectable WSBackendMessageLogEntity wsBackendMessageLogEntity) {
        new Expectations() {{

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsBackendMessageLogEntity.getType();
            result = WSBackendMessageType.SUBMIT_MESSAGE;

            wsBackendMessageLogEntity.getEntityId();
            result = ID;

            wsPluginDispatchRulesService.getRule(RULE_NAME);
            result = null;
        }};

        wsPluginMessageSender.sendNotification(wsBackendMessageLogEntity);

        new FullVerifications() {{
        }};
    }

    @Test
    public void sendSubmitMessage(@Injectable WSBackendMessageLogEntity wsBackendMessageLogEntity,
                                   @Injectable SOAPMessage soapMessage,
                                   @Injectable WSPluginDispatchRule wsPluginDispatchRule,
                                  @Injectable TransformerFactory transformerFactory) throws MessageNotFoundException, TransformerException {
        new Expectations() {{

            wsPluginMessageBuilder.buildSOAPMessage(wsBackendMessageLogEntity);
            result = soapMessage;
            times = 1;

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsBackendMessageLogEntity.getType();
            result = WSBackendMessageType.SUBMIT_MESSAGE;

            wsBackendMessageLogEntity.getEntityId();
            result = ID;
            wsBackendMessageLogEntity.getMessageId();
            result = MESSAGE_ID;

            wsPluginDispatchRulesService.getRule(RULE_NAME);
            result = wsPluginDispatchRule;

            wsPluginDispatchRule.getEndpoint();
            result = END_POINT;

            wsPluginDispatcher.dispatch(soapMessage, END_POINT);
            result = soapMessage;
            times = 1;

            transformerFactory.newTransformer().transform((Source) any, (Result) any);

            xmlUtil.getTransformerFactory();
        }};

        wsPluginMessageSender.sendNotification(wsBackendMessageLogEntity);

        new FullVerifications() {{
            wsBackendMessageLogEntity.setBackendMessageStatus(WSBackendMessageStatus.SENT);
            times = 1;
            wsPlugin.downloadMessage(MESSAGE_ID, null);
            times = 1;
        }};
    }

    @Test
    public void sendMessageSuccess(@Injectable WSBackendMessageLogEntity wsBackendMessageLogEntity,
                                   @Injectable SOAPMessage soapMessage,
                                   @Injectable WSPluginDispatchRule wsPluginDispatchRule,
                                   @Injectable TransformerFactory transformerFactory) throws TransformerException {
        new Expectations() {{

            wsPluginMessageBuilder.buildSOAPMessage(wsBackendMessageLogEntity);
            result = soapMessage;
            times = 1;

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsBackendMessageLogEntity.getType();
            result = WSBackendMessageType.SEND_SUCCESS;

            xmlUtil.getTransformerFactory();

            soapMessage.getSOAPPart();

            wsBackendMessageLogEntity.getEntityId();
            result = ID;

            wsBackendMessageLogEntity.getMessageId();
            result = MESSAGE_ID;

            wsPluginDispatchRulesService.getRule(RULE_NAME);
            result = wsPluginDispatchRule;

            wsPluginDispatchRule.getEndpoint();
            result = END_POINT;

            wsPluginDispatcher.dispatch(soapMessage, END_POINT);
            result = soapMessage;
            times = 1;
        }};

        wsPluginMessageSender.sendNotification(wsBackendMessageLogEntity);

        new FullVerifications() {{
            wsBackendMessageLogEntity.setBackendMessageStatus(WSBackendMessageStatus.SENT);
            times = 1;
        }};
    }

    @Test
    public void sendMessageSuccess_exception(
            @Injectable WSBackendMessageLogEntity wsBackendMessageLogEntity,
            @Injectable SOAPMessage soapMessage,
            @Injectable WSPluginDispatchRule wsPluginDispatchRule) {
        new Expectations() {{
            wsPluginMessageBuilder.buildSOAPMessage(wsBackendMessageLogEntity);
            result = soapMessage;
            times = 1;

            wsBackendMessageLogEntity.getRuleName();
            result = RULE_NAME;

            wsBackendMessageLogEntity.getType();
            result = WSBackendMessageType.SEND_SUCCESS;

            wsBackendMessageLogEntity.getEntityId();
            result = ID;

            wsPluginDispatchRulesService.getRule(RULE_NAME);
            result = wsPluginDispatchRule;

            wsPluginDispatchRule.getEndpoint();
            result = END_POINT;

            wsPluginDispatcher.dispatch(soapMessage, END_POINT);
            result = new IllegalStateException("ERROR");
            times = 1;
        }};

        wsPluginMessageSender.sendNotification(wsBackendMessageLogEntity);

        new FullVerifications() {{
            reliabilityService.handleReliability(wsBackendMessageLogEntity, wsPluginDispatchRule);
            times = 1;
        }};
    }
}