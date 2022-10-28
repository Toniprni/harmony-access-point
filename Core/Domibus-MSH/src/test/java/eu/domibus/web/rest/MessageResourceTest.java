package eu.domibus.web.rest;

import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.usermessage.UserMessageRestoreService;
import eu.domibus.api.usermessage.UserMessageService;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.message.MessagesLogService;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.converter.MessageConverterService;
import eu.domibus.web.rest.error.ErrorHandlerService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.UUID;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@RunWith(JMockit.class)
public class MessageResourceTest {

    @Tested
    MessageResource messageResource;

    @Injectable
    UserMessageService userMessageService;

    @Injectable
    MessageConverterService messageConverterService;

    @Injectable
    private UserMessageLogDao userMessageLogDao;

    @Injectable
    private AuditService auditService;

    @Injectable
    MessagesLogService messagesLogService;

    @Injectable
    ErrorHandlerService errorHandlerService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private UserMessageRestoreService userMessageDefaultRestoreService;

    @Injectable
    RequestFilterUtils requestFilterUtils;

    @Injectable
    AuthUtils authUtils;

    @Test
    public void testDownloadZipped() throws IOException {
        // Given
        new Expectations() {{
            userMessageService.getMessageWithAttachmentsAsZip(anyString, MSHRole.SENDING);
            result = new byte[]{0, 1, 2};
        }};

        ResponseEntity<ByteArrayResource> responseEntity = null;
        try {
            // When
            responseEntity = messageResource.downloadUserMessage("messageId", MSHRole.SENDING);
        } catch (IOException | MessageNotFoundException e) {
            // NOT Then :)
            Assert.fail("Exception in zipFiles method");
        }

        // Then
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assert.assertEquals("application/zip", responseEntity.getHeaders().get("Content-Type").get(0));
        Assert.assertEquals("attachment; filename=messageId.zip", responseEntity.getHeaders().get("content-disposition").get(0));
    }

    @Test
    public void testReSend() {
        String messageId = UUID.randomUUID().toString();
        messageResource.resend(messageId);
        new Verifications() {{
            final String messageIdActual;
            final String messageIdActual1;
            userMessageDefaultRestoreService.resendFailedOrSendEnqueuedMessage(messageIdActual = withCapture());
            times = 1;
            Assert.assertEquals(messageId, messageIdActual);
        }};
    }

    @Test
    public void getByteArrayResourceResponseEntity_empty() {
        String messageId = "messageId";
        new Expectations() {{
            userMessageService.getMessageEnvelopesAsZip(messageId, MSHRole.SENDING);
            result = new byte[]{};
        }};

        ResponseEntity<ByteArrayResource> result = messageResource.getByteArrayResourceResponseEntity(messageId, MSHRole.SENDING);
        Assert.assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    public void getByteArrayResourceResponseEntity() {
        String messageId = "messageId";
        byte[] content = {1, 2, 3, 4};
        new Expectations() {{
            userMessageService.getMessageEnvelopesAsZip(messageId, MSHRole.SENDING);
            this.result = content;
        }};

        ResponseEntity<ByteArrayResource> result = messageResource.getByteArrayResourceResponseEntity(messageId, MSHRole.SENDING);
        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());
        Assert.assertEquals(content, result.getBody().getByteArray());
        Assert.assertEquals("application/zip", result.getHeaders().get("Content-Type").get(0));
    }

}
