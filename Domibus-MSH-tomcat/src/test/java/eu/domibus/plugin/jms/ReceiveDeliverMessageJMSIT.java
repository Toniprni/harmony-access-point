
package eu.domibus.plugin.jms;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import eu.domibus.AbstractBackendJMSIT;
import eu.domibus.api.model.*;
import eu.domibus.core.message.MessagingService;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.core.message.UserMessageLogDefaultService;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.plugin.webService.generated.MshRole;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;

import javax.activation.DataHandler;
import javax.jms.ConnectionFactory;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.domibus.plugin.jms.JMSMessageConstants.MESSAGE_ID;


/**
 * This class implements the test cases Receive Deliver Message-01
 *
 * @author martifp
 */
@Ignore
public class ReceiveDeliverMessageJMSIT extends AbstractBackendJMSIT {


  /*  @Autowired
    private JMSPluginImpl JMSPluginImpl;

    @Autowired
    private ConnectionFactory jmsConnectionFactory;


    @Autowired
    MessagingService messagingService;

    @Autowired
    UserMessageLogDefaultService userMessageLogService;

    @Autowired
    MessageSubtypeDao messageSubtypeDao;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    @Before
    public void before() throws IOException, XmlProcessingException {
        uploadPmode(wireMockRule.port());
    }

    *//**
     * It tests the message reception by Domibus through the JMS channel.
     * It also checks that the messages are actually pushed on the right queues (dispatch and reply).
     * The message ID is cleaned to simulate the submission of the a new message.
     *
     * @throws Exception
     *//*
    @Test
    @DirtiesContext
    @Rollback
    public void testReceiveMessage() throws Exception {
        final MapMessage mapMessage = prepareMessageForSubmit();

//        super.prepareSendMessage("validAS4Response.xml");

        System.out.println("MapMessage: " + mapMessage);
        String messageId = UUID.randomUUID().toString();
        mapMessage.setStringProperty(MESSAGE_ID, messageId); // Cleaning the message ID since it is supposed to submit a new message.
        mapMessage.setStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY, JMSMessageConstants.MESSAGE_TYPE_SUBMIT);
        // The downloaded MapMessage is used as input parameter for the real Test case here!
        JMSPluginImpl.receiveMessage(mapMessage);
        // Verifies that the message is really in the queue
        javax.jms.Connection connection = jmsConnectionFactory.createConnection("domibus", "changeit");
        connection.start();
        Message message = popQueueMessageWithTimeout(connection, JMS_BACKEND_REPLY_QUEUE_NAME, 2000);
        connection.close();
        Assert.assertEquals(message.getStringProperty(JMSMessageConstants.MESSAGE_ID), messageId);
        Assert.assertNull(message.getStringProperty("ErrorMessage"));

    }

    protected MapMessage prepareMessageForSubmit() throws Exception {
        String messageId = "2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu";
        final UserMessage userMessage = getUserMessageTemplate();
        userMessage.setAction("TC3Leg1");

        String messagePayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<hello>world</hello>";
        final PartInfo partInfo = userMessage.getPayloadInfo().getPartInfo().iterator().next();
        partInfo.setBinaryData(messagePayload.getBytes());
        partInfo.setHref("message");
        final Property mimeTypeProperty = new Property();
        mimeTypeProperty.setName(Property.MIME_TYPE);
        mimeTypeProperty.setValue("text/xml");
        partInfo.getPartProperties().add(mimeTypeProperty);

        partInfo.setPayloadDatahandler(new DataHandler(new ByteArrayDataSource(messagePayload.getBytes(), "text/xml")));
        userMessage.setMessageId(messageId);
        Messaging messaging = new Messaging();
        messaging.setUserMessage(userMessage);
        messagingService.storeMessage(messaging, MSHRole.RECEIVING, null, "backendWebservice");

        UserMessageLog userMessageLog = new UserMessageLog();
        userMessageLog.setMessageStatus(MessageStatus.RECEIVED);
        userMessageLog.setUserMessage(userMessage);
        userMessageLog.setMessageType(MessageType.USER_MESSAGE);
        userMessageLog.setMshRole(MSHRole.RECEIVING);
        userMessageLog.setReceived(new Date());
        userMessageLogService.save(messageId,
                eu.domibus.common.MessageStatus.RECEIVED.name(),
                NotificationStatus.REQUIRED.name(),
                MshRole.RECEIVING.name(),
                1,
                "default",
                "backendWebservice",
                "",
                null,
                null,
                null,
                null);


        javax.jms.Connection connection = jmsConnectionFactory.createConnection("domibus", "changeit");
        connection.start();
        // Puts the message in the notification queue so it can be downloaded
        pushQueueMessage(messageId, connection, JMS_NOT_QUEUE_NAME);
        connection.close();

        final MapMessage mapMessage = new ActiveMQMapMessage();

        JMSPluginImpl.downloadMessage(messageId, mapMessage);
        return mapMessage;
    }

    *//**
     * Similar test to the previous one but this does not change the Message ID so that an exception is raised and handled with an JMS error message.
     * It tests that the message is actually into the REPLY queue.
     *
     * @throws Exception
     *//*
    @Test
    @DirtiesContext
    @Rollback
    public void testDuplicateMessage() throws Exception {
        final MapMessage mapMessage = prepareMessageForSubmit();
//        super.prepareSendMessage("validAS4Response.xml");

        final String messageId = mapMessage.getStringProperty(MESSAGE_ID);

        System.out.println("MapMessage: " + mapMessage);
        mapMessage.setStringProperty(JMSMessageConstants.JMS_BACKEND_MESSAGE_TYPE_PROPERTY_KEY, JMSMessageConstants.MESSAGE_TYPE_SUBMIT);
        // The downloaded MapMessage is used as input parameter for the real Test case here!
        JMSPluginImpl.receiveMessage(mapMessage);
        // Verifies that the message is really in the queue
        javax.jms.Connection connection = jmsConnectionFactory.createConnection("domibus", "changeit");
        connection.start();
        Message message = popQueueMessageWithTimeout(connection, JMS_BACKEND_REPLY_QUEUE_NAME, 2000);
        connection.close();
        Assert.assertEquals(message.getStringProperty(JMSMessageConstants.MESSAGE_ID), messageId);
        Assert.assertNotNull(message.getStringProperty("ErrorMessage"));
    }*/

}
