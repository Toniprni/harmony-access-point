package eu.domibus.core.message;


import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.UserMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.BackendConnector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author idragusa
 * @since 5.0
 */
@Transactional
public class DeleteSentFailedMessageIT extends DeleteMessageAbstractIT {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DeleteSentFailedMessageIT.class);

    @Autowired
    private UserMessageDao userMessageDao;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Transactional
    @Before
    public void updatePmodeForSendFailure() throws IOException, XmlProcessingException {
        Map<String, String> toReplace = new HashMap<>();
        toReplace.put("retry=\"12;4;CONSTANT\"", "retry=\"1;0;CONSTANT\"");
        uploadPmode(SERVICE_PORT, toReplace);
    }

    @Test
    public void testDeleteFailedMessage() throws MessagingProcessingException {
        deleteAllMessages();

        BackendConnector backendConnector = Mockito.mock(BackendConnector.class);
        Mockito.when(backendConnectorProvider.getBackendConnector(Mockito.any(String.class))).thenReturn(backendConnector);

        String messageId = itTestsService.sendMessageWithStatus(MessageStatus.SEND_FAILURE);

        LOG.info("Message Id to delete: [{}]", messageId);
        UserMessage byMessageId = userMessageDao.findByMessageId(messageId);
        Assert.assertNotNull(byMessageId);

        Assert.assertNotNull(userMessageDao.findByEntityId(byMessageId.getEntityId()));
        Assert.assertNotNull(userMessageLogDao.findByEntityIdSafely(byMessageId.getEntityId()));

        deleteAllMessages();

        Assert.assertNull(userMessageDao.findByMessageId(messageId));
        Assert.assertNull(userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING));
    }

}
