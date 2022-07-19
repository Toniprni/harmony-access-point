package eu.domibus.ext.delegate.services.message;

import eu.domibus.api.message.UserMessageSecurityService;
import eu.domibus.api.message.acknowledge.MessageAcknowledgeService;
import eu.domibus.api.message.acknowledge.MessageAcknowledgement;
import eu.domibus.api.model.MSHRole;
import eu.domibus.ext.delegate.mapper.MessageExtMapper;
import eu.domibus.ext.domain.MessageAcknowledgementDTO;
import eu.domibus.ext.exceptions.AuthenticationExtException;
import eu.domibus.ext.exceptions.MessageAcknowledgeExtException;
import eu.domibus.ext.services.MessageAcknowledgeExtService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @author migueti, Cosmin Baciu
 * @since 3.3
 */
@Service
public class MessageAcknowledgeServiceDelegate implements MessageAcknowledgeExtService {

    protected eu.domibus.api.message.acknowledge.MessageAcknowledgeService messageAcknowledgeCoreService;

    protected MessageExtMapper messageExtMapper;

    protected UserMessageSecurityService userMessageSecurityService;

    public MessageAcknowledgeServiceDelegate(MessageAcknowledgeService messageAcknowledgeCoreService,
                                             MessageExtMapper messageExtMapper,
                                             UserMessageSecurityService userMessageSecurityService) {
        this.messageAcknowledgeCoreService = messageAcknowledgeCoreService;
        this.messageExtMapper = messageExtMapper;
        this.userMessageSecurityService = userMessageSecurityService;
    }


    @Override
    public MessageAcknowledgementDTO acknowledgeMessageDelivered(String messageId, Timestamp acknowledgeTimestamp, Map<String, String> properties) throws AuthenticationExtException, MessageAcknowledgeExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.RECEIVING);

        final MessageAcknowledgement messageAcknowledgement = messageAcknowledgeCoreService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp, properties);
        return messageExtMapper.messageAcknowledgementToMessageAcknowledgementDTO(messageAcknowledgement);
    }

    @Override
    public MessageAcknowledgementDTO acknowledgeMessageDeliveredWithUnsecureLoginAllowed(String messageId, Timestamp acknowledgeTimestamp) throws AuthenticationExtException, MessageAcknowledgeExtException {
        userMessageSecurityService.checkMessageAuthorizationWithUnsecureLoginAllowed(messageId, MSHRole.RECEIVING);

        final MessageAcknowledgement messageAcknowledgement = messageAcknowledgeCoreService.acknowledgeMessageDelivered(messageId, acknowledgeTimestamp, null);
        return messageExtMapper.messageAcknowledgementToMessageAcknowledgementDTO(messageAcknowledgement);
    }

    @Override
    public MessageAcknowledgementDTO acknowledgeMessageProcessed(String messageId, Timestamp acknowledgeTimestamp, Map<String, String> properties) throws AuthenticationExtException, MessageAcknowledgeExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId, MSHRole.RECEIVING);

        final MessageAcknowledgement messageAcknowledgement = messageAcknowledgeCoreService.acknowledgeMessageProcessed(messageId, acknowledgeTimestamp, properties);
        return messageExtMapper.messageAcknowledgementToMessageAcknowledgementDTO(messageAcknowledgement);
    }

    @Override
    public List<MessageAcknowledgementDTO> getAcknowledgedMessages(String messageId) throws AuthenticationExtException, MessageAcknowledgeExtException {
        userMessageSecurityService.checkMessageAuthorization(messageId);

        final List<MessageAcknowledgement> messageAcknowledgement = messageAcknowledgeCoreService.getAcknowledgedMessages(messageId);
        return messageExtMapper.messageAcknowledgementToMessageAcknowledgementDTO(messageAcknowledgement);

    }

}
