package eu.domibus.core.message.testservice;

import com.google.gson.Gson;
import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.model.SignalMessage;
import eu.domibus.api.model.UserMessageLog;
import eu.domibus.common.model.configuration.Agreement;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.error.ErrorLogDao;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.message.UserMessageLogDao;
import eu.domibus.core.message.signal.SignalMessageDao;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.core.plugin.handler.DatabaseMessageHandler;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;
import eu.domibus.web.rest.ro.TestServiceMessageInfoRO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.NoResultException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Cosmin Baciu
 * @author Ion Perpegel
 * @since 4.0
 */
@Service
public class TestService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TestService.class);

    private static final String TEST_PAYLOAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>world</hello>";

    private static final String MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT = "finalRecipient";

    private static final String BACKEND_NAME = "TestService";

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private DatabaseMessageHandler databaseMessageHandler;

    @Autowired
    private UserMessageLogDao userMessageLogDao;

    @Autowired
    private SignalMessageLogDao signalMessageLogDao;

    @Autowired
    private SignalMessageDao signalMessageDao;

    @Autowired
    private ErrorLogDao errorLogDao;

    public String submitTest(String sender, String receiver) throws IOException, MessagingProcessingException {
        LOG.info("Submitting test message from [{}] to [{}]", sender, receiver);

        Submission messageData = createSubmission(sender);

        // Set Receiver
        messageData.getToParties().clear();
        messageData.addToParty(receiver, pModeProvider.getPartyIdType(receiver));

        return databaseMessageHandler.submit(messageData, BACKEND_NAME);
    }

    public String submitTestDynamicDiscovery(String sender, String receiver, String receiverType) throws MessagingProcessingException, IOException {
        LOG.info("Submitting test message with dynamic discovery from [{}] to [{}] with type [{}]", sender, receiver, receiverType);

        Submission messageData = createSubmission(sender);

        // Clears Receivers
        messageData.getToParties().clear();

        // Set Final Recipient Value and Type
        for (Submission.TypedProperty property : messageData.getMessageProperties()) {
            if (property.getKey().equals(MESSAGE_PROPERTY_KEY_FINAL_RECIPIENT)) {
                property.setValue(receiver);
                property.setType(receiverType);
            }
        }

        return databaseMessageHandler.submit(messageData, BACKEND_NAME);
    }

    protected Submission createSubmission(String sender) throws IOException {
        Resource testServiceFile = new ClassPathResource("messages/testservice/testservicemessage.json");
        String jsonStr = new String(IOUtils.toByteArray(testServiceFile.getInputStream()), StandardCharsets.UTF_8);

        Submission submission = new Gson().fromJson(jsonStr, Submission.class);

        DataHandler payLoadDataHandler = new DataHandler(new ByteArrayDataSource(TEST_PAYLOAD.getBytes(), "text/xml"));
        Submission.TypedProperty objTypedProperty = new Submission.TypedProperty("MimeType", "text/xml");
        Collection<Submission.TypedProperty> listTypedProperty = new ArrayList<>();
        listTypedProperty.add(objTypedProperty);
        Submission.Payload objPayload1 = new Submission.Payload("cid:message", payLoadDataHandler, listTypedProperty, false, null, null);
        submission.addPayload(objPayload1);

        // Set Sender
        submission.getFromParties().clear();
        submission.addFromParty(sender, pModeProvider.getPartyIdType(sender));

        // Set ServiceType
        submission.setServiceType(pModeProvider.getServiceType(Ebms3Constants.TEST_SERVICE));

        // Set From Role
        submission.setFromRole(pModeProvider.getRole("INITIATOR", Ebms3Constants.TEST_SERVICE));

        // Set To Role
        submission.setToRole(pModeProvider.getRole("RESPONDER", Ebms3Constants.TEST_SERVICE));

        // Set Agreement Ref
        Agreement agreementRef = pModeProvider.getAgreementRef(Ebms3Constants.TEST_SERVICE);
        if(agreementRef != null) {
            submission.setAgreementRef(agreementRef.getValue());
            submission.setAgreementRefType(agreementRef.getType());
        }

        // Set Conversation Id
        // As the eb:ConversationId element is required it must always have a value.
        // If no value is included in the 301 submission of the business document to the Access Point,
        // the Access Point MUST set the value of 302 eb:ConversationId to “1” as specified in section 4.3 of [ebMS3CORE].
        submission.setConversationId("1");

        return submission;
    }

    /**
     * This method is to get the last test Sent User Message for the given party Id,
     * including errors if not found
     *
     * @param partyId
     * @return TestServiceMessageInfoRO
     * @throws TestServiceException
     */
    public TestServiceMessageInfoRO getLastTestSentWithErrors(String partyId) throws TestServiceException {
        TestServiceMessageInfoRO result = getLastTestSent(partyId);
        if (result == null) {
            throw new TestServiceException(DomibusCoreErrorCode.DOM_001, "No User message found for party [" + partyId + "]");
        }

        if (result.getTimeReceived() == null) {
            String errorDetails = getErrorsDetails(result.getMessageId());
            throw new TestServiceException("No User Message found. Error details are: " + errorDetails);
        }

        return result;
    }

    /**
     * This method retrieves the last test Sent User Message for the given party Id
     *
     * @param partyId
     * @return TestServiceMessageInfoRO
     * @throws TestServiceException
     */
    public TestServiceMessageInfoRO getLastTestSent(String partyId) {
        LOG.debug("Getting last sent test message for partyId [{}]", partyId);

        String userMessageId = userMessageLogDao.findLastTestMessageId(partyId);
        if (StringUtils.isBlank(userMessageId)) {
            LOG.debug("Could not find last user message id for party [{}]", partyId);
            return null;
        }

        UserMessageLog userMessageLog = userMessageLogDao.findByMessageIdSafely(userMessageId);

        return getTestServiceMessageInfoRO(partyId, userMessageId, userMessageLog);
    }

    protected TestServiceMessageInfoRO getTestServiceMessageInfoRO(String partyId, String userMessageId, UserMessageLog userMessageLog) {
        TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
        testServiceMessageInfoRO.setMessageId(userMessageId);
        testServiceMessageInfoRO.setPartyId(partyId);
        Party party = pModeProvider.getPartyByIdentifier(partyId);
        testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());
        if (userMessageLog != null) {
            testServiceMessageInfoRO.setMessageStatus(userMessageLog.getMessageStatus());
            testServiceMessageInfoRO.setTimeReceived(userMessageLog.getReceived());
        }
        return testServiceMessageInfoRO;
    }

    /**
     * This method is to get the last Received Signal Message for a test message for the given party Id and User MessageId,
     * including errors if an acceptable signal message cannot be found.
     *
     * @param partyId, userMessageId
     * @return TestServiceMessageInfoRO
     * @throws TestServiceException
     */
    public TestServiceMessageInfoRO getLastTestReceivedWithErrors(String partyId, String userMessageId) throws TestServiceException {
        TestServiceMessageInfoRO result = getLastTestReceived(partyId, userMessageId);
        if (result == null) {
            String errorDetails = getErrorsDetails(userMessageId);
            throw new TestServiceException("No Signal Message found." + errorDetails);
        }

        return result;
    }

    /**
     * This method retrieves the last Received Signal Message for a test message for the given party Id and User MessageId
     *
     * @param partyId, userMessageId
     * @return TestServiceMessageInfoRO
     */
    public TestServiceMessageInfoRO getLastTestReceived(String partyId, String userMessageId) {
        LOG.debug("Getting last received signal for a test message from partyId [{}]", partyId);

        SignalMessage signalMessage;

        if (StringUtils.isNotBlank(userMessageId)) {
            // if userMessageId is provided, try to find its signal message
            signalMessage = signalMessageDao.findByUserMessageIdWithUserMessage(userMessageId);
            if (signalMessageDao == null) {
                LOG.debug("Could not find messaging for message ID [{}]", userMessageId);
                return null;
            }
        } else {
            // if userMessageId is not provided, find the most recent signal message received for a test message
            String signalMessageId = signalMessageLogDao.findLastTestMessageId(partyId);
            if (signalMessageId == null) {
                LOG.debug("Could not find any signal message from party [{}]", partyId);
                return null;
            }
            signalMessage = signalMessageDao.findBySignalMessageId(signalMessageId);
        }

        if (signalMessage == null) {
            LOG.debug("Could not find signal message from partyId [{}] with userMessageId [{}]", partyId, userMessageId);
            return null;
        }

        return getTestServiceMessageInfoRO(partyId, signalMessage);
    }

    protected String getErrorsDetails(String userMessageId) {
        String result;
        String errorDetails = getErrorsForMessage(userMessageId);
        if (StringUtils.isEmpty(errorDetails)) {
            result = "Please call the method again to see the details.";
        } else {
            result = "Error details are: " + errorDetails;
        }
        return result;
    }

    protected String getErrorsForMessage(String userMessageId) {
        List<ErrorLogEntry> errorLogEntries = errorLogDao.getErrorsForMessage(userMessageId);
        String errors = errorLogEntries.stream()
                .map(err -> err.getErrorCode().getErrorCodeName() + "-" + err.getErrorDetail())
                .collect(Collectors.joining(", "));
        return errors;
    }

    protected TestServiceMessageInfoRO getTestServiceMessageInfoRO(String partyId, SignalMessage signalMessage) {
        TestServiceMessageInfoRO testServiceMessageInfoRO = new TestServiceMessageInfoRO();
        if (signalMessage != null) {
            testServiceMessageInfoRO.setMessageId(signalMessage.getSignalMessageId());
            testServiceMessageInfoRO.setTimeReceived(signalMessage.getTimestamp());
        }
        Party party = pModeProvider.getPartyByIdentifier(partyId);
        testServiceMessageInfoRO.setPartyId(partyId);
        testServiceMessageInfoRO.setAccessPoint(party.getEndpoint());

        return testServiceMessageInfoRO;
    }

}
