package eu.domibus.plugin.webService.backend.reliability.retry;

import eu.domibus.ext.domain.JMSMessageDTOBuilder;
import eu.domibus.ext.domain.JmsMessageDTO;
import eu.domibus.ext.services.JMSExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.webService.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.webService.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.webService.backend.WSBackendMessageType;
import eu.domibus.plugin.webService.backend.reliability.queue.WSSendMessageListener;
import eu.domibus.plugin.webService.backend.rules.WSPluginDispatchRule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import java.util.ArrayList;
import java.util.List;

import static eu.domibus.plugin.webService.backend.reliability.queue.WSMessageListenerContainerConfiguration.WS_PLUGIN_SEND_QUEUE;

/**
 * @author Francois Gautier
 * @since 5.0
 */
@Service
public class WSPluginBackendRetryService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSPluginBackendRetryService.class);

    private final WSBackendMessageLogDao wsBackendMessageLogDao;

    protected JMSExtService jmsExtService;

    protected Queue wsPluginSendQueue;

    public WSPluginBackendRetryService(WSBackendMessageLogDao wsBackendMessageLogDao,
                                       JMSExtService jmsExtService,
                                       @Qualifier(WS_PLUGIN_SEND_QUEUE) Queue wsPluginSendQueue) {
        this.wsBackendMessageLogDao = wsBackendMessageLogDao;
        this.jmsExtService = jmsExtService;
        this.wsPluginSendQueue = wsPluginSendQueue;
    }

    public List<WSBackendMessageLogEntity> getMessagesNotAlreadyScheduled() {
        List<WSBackendMessageLogEntity> result = new ArrayList<>();

        final List<WSBackendMessageLogEntity> messageIdsToSend = wsBackendMessageLogDao.findRetryMessages();
        if (CollectionUtils.isEmpty(messageIdsToSend)) {
            LOG.trace("No backend message found to be resend");
            return result;
        }
        LOG.trace("Found [{}] backend messages to be send.", messageIdsToSend.size());

        return messageIdsToSend;
    }

    @Transactional
    public void sendWaitingForRetry() {
        try {
            final List<WSBackendMessageLogEntity> messagesNotAlreadyQueued = getMessagesNotAlreadyScheduled();

            for (final WSBackendMessageLogEntity backendMessage : messagesNotAlreadyQueued) {
                sendToQueue(backendMessage);
            }
        } catch (Exception e) {
            LOG.error("Error while sending notifications.", e);
        }
    }

    protected void sendToQueue(WSBackendMessageLogEntity backendMessage) {
        LOG.debug("Send backendMessage [{}] to queue [{}]", backendMessage.getEntityId(), getQueueName());
        final JmsMessageDTO jmsMessage = JMSMessageDTOBuilder.
                create()
                .property(MessageConstants.MESSAGE_ID, backendMessage.getMessageId())
                .property(WSSendMessageListener.ID, backendMessage.getEntityId())
                .property(WSSendMessageListener.TYPE, backendMessage.getType().name())
                .build();
        jmsExtService.sendMessageToQueue(jmsMessage, wsPluginSendQueue);
        backendMessage.setScheduled(true);
    }

    private String getQueueName() {
        try {
            return wsPluginSendQueue.getQueueName();
        } catch (JMSException e) {
            LOG.trace("wsPluginSendQueue name not found", e);
            return null;
        }
    }

    @Transactional
    public void send(String messageId, String finalRecipient, String originalSender, WSPluginDispatchRule rule, WSBackendMessageType messageType) {
        WSBackendMessageLogEntity backendMessage = getWsBackendMessageLogEntity(messageId, messageType, finalRecipient, originalSender, rule);
        WSBackendMessageLogEntity persistedBackendMessage = wsBackendMessageLogDao.createEntity(backendMessage);
        sendToQueue(persistedBackendMessage);
    }

    @Transactional
    public void send(List<String> messageIds, String finalRecipient, WSPluginDispatchRule rule, WSBackendMessageType messageType) {
        WSBackendMessageLogEntity backendMessage = getWsBackendMessageLogEntity(null, messageType, finalRecipient, null, rule);
        WSBackendMessageLogEntity persistedBackendMessage = wsBackendMessageLogDao.createEntity(backendMessage);
        LOG.info("[{}] backend message id [{}] for [{}] messagesIds [{}]", messageType, persistedBackendMessage.getEntityId(), messageIds.size(), messageIds);
        sendToQueue(persistedBackendMessage);
    }

    protected WSBackendMessageLogEntity getWsBackendMessageLogEntity(
            String messageId,
            WSBackendMessageType messageType,
            String finalRecipient,
            String originalSender,
            WSPluginDispatchRule rule) {
        WSBackendMessageLogEntity wsBackendMessageLogEntity = new WSBackendMessageLogEntity();
        wsBackendMessageLogEntity.setMessageId(messageId);
        wsBackendMessageLogEntity.setRuleName(rule.getRuleName());
        wsBackendMessageLogEntity.setFinalRecipient(finalRecipient);
        wsBackendMessageLogEntity.setOriginalSender(originalSender);
        wsBackendMessageLogEntity.setType(messageType);
        wsBackendMessageLogEntity.setSendAttempts(0);
        wsBackendMessageLogEntity.setSendAttemptsMax(rule.getRetryCount());
        return wsBackendMessageLogEntity;
    }
}
