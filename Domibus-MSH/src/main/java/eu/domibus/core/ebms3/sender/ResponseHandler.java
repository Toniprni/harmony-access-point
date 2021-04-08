package eu.domibus.core.ebms3.sender;

import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.api.exceptions.DomibusDateTimeException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MSHRoleEntity;
import eu.domibus.api.model.SignalMessageResult;
import eu.domibus.api.model.UserMessage;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.error.ErrorLogDao;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.message.MshRoleDao;
import eu.domibus.core.message.nonrepudiation.NonRepudiationService;
import eu.domibus.core.message.signal.SignalMessageDao;
import eu.domibus.core.message.signal.SignalMessageLogDefaultService;
import eu.domibus.core.replication.UIReplicationSignalService;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

/**
 * @author Christian Koch, Stefan Mueller, Federico Martini
 * @author Cosmin Baciu
 */
@Service
public class ResponseHandler {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(ResponseHandler.class);

    private final SignalMessageLogDefaultService signalMessageLogDefaultService;
    private final UIReplicationSignalService uiReplicationSignalService;
    private final NonRepudiationService nonRepudiationService;
    private final SignalMessageDao signalMessageDao;
    protected final MessageUtil messageUtil;
    MshRoleDao mshRoleDao;
    private final ErrorLogDao errorLogDao;
    protected Ebms3Converter ebms3Converter;

    public ResponseHandler(SignalMessageLogDefaultService signalMessageLogDefaultService,
                           UIReplicationSignalService uiReplicationSignalService,
                           NonRepudiationService nonRepudiationService,
                           SignalMessageDao signalMessageDao,
                           MessageUtil messageUtil,
                           MshRoleDao mshRoleDao,
                           ErrorLogDao errorLogDao,
                           Ebms3Converter ebms3Converter) {
        this.signalMessageLogDefaultService = signalMessageLogDefaultService;
        this.uiReplicationSignalService = uiReplicationSignalService;
        this.nonRepudiationService = nonRepudiationService;
        this.signalMessageDao = signalMessageDao;
        this.messageUtil = messageUtil;
        this.mshRoleDao = mshRoleDao;
        this.errorLogDao = errorLogDao;
        this.ebms3Converter = ebms3Converter;
    }

    public ResponseResult verifyResponse(final SOAPMessage response, String messageId) throws EbMS3Exception {
        LOGGER.debug("Verifying response");

        ResponseResult result = new ResponseResult();

        final Ebms3Messaging ebms3Messaging;
        try {
            ebms3Messaging = messageUtil.getMessagingWithDom(response);
            result.setResponseMessaging(ebms3Messaging);
        } catch (SOAPException | DomibusDateTimeException ex) {
            throw EbMS3ExceptionBuilder
                    .getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0004)
                    .errorDetail("Problem occurred during marshalling")
                    .refToMessageId(messageId)
                    .mshRole(MSHRole.SENDING)
                    .cause(ex)
                    .build();
        }

        final Ebms3SignalMessage ebms3SignalMessage = ebms3Messaging.getSignalMessage();
        final ResponseStatus responseStatus = getResponseStatus(ebms3SignalMessage);
        result.setResponseStatus(responseStatus);

        return result;
    }

    public void saveResponse(final SOAPMessage response, final UserMessage userMessage, final Ebms3Messaging ebms3MessagingResponse) {
        SignalMessageResult signalMessageResult = ebms3Converter.convertFromEbms3(ebms3MessagingResponse);

        final eu.domibus.api.model.SignalMessage signalMessage = signalMessageResult.getSignalMessage();
        signalMessage.setUserMessage(userMessage);
        nonRepudiationService.saveResponse(response, signalMessage);

        // Stores the signal message
        signalMessageDao.create(signalMessage);

        // Builds the signal message log
        // Updating the reference to the signal message
        String userMessageService = userMessage.getService().getValue();
        String userMessageAction = userMessage.getActionValue();

        signalMessageLogDefaultService.save(signalMessage, userMessageService, userMessageAction);

        final MSHRoleEntity sendingRole = mshRoleDao.findByRole(MSHRole.SENDING);
        createWarningEntries(ebms3MessagingResponse.getSignalMessage(), sendingRole);

        //UI replication
        uiReplicationSignalService.signalMessageReceived(signalMessage.getSignalMessageId());
    }

    protected void createWarningEntries(Ebms3SignalMessage signalMessage, MSHRoleEntity mshRoleEntity) {
        if (signalMessage.getError() == null || signalMessage.getError().isEmpty()) {
            LOGGER.debug("No warning entries to create");
            return;
        }

        LOGGER.debug("Creating warning entries");

        for (final Ebms3Error error : signalMessage.getError()) {
            if (ErrorCode.SEVERITY_WARNING.equalsIgnoreCase(error.getSeverity())) {
                final String errorCode = error.getErrorCode();
                final String errorDetail = error.getErrorDetail();
                final String refToMessageInError = error.getRefToMessageInError();

                LOGGER.warn("Creating warning error with error code [{}], error detail [{}] and refToMessageInError [{}]", errorCode, errorDetail, refToMessageInError);

                EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.findErrorCodeBy(errorCode), errorDetail, refToMessageInError, null);
                final ErrorLogEntry errorLogEntry = new ErrorLogEntry(ebMS3Ex, mshRoleEntity);
                this.errorLogDao.create(errorLogEntry);
            }
        }
    }

    protected ResponseStatus getResponseStatus(Ebms3SignalMessage ebms3SignalMessage) throws EbMS3Exception {
        LOGGER.debug("Getting response status");

        // Checks if the signal message is Ok
        if (ebms3SignalMessage.getError() == null || ebms3SignalMessage.getError().isEmpty()) {
            LOGGER.debug("Response message contains no errors");
            return ResponseStatus.OK;
        }

        for (final Ebms3Error ebms3Error : ebms3SignalMessage.getError()) {
            if (ErrorCode.SEVERITY_FAILURE.equalsIgnoreCase(ebms3Error.getSeverity())) {
                EbMS3Exception ebMS3Ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.findErrorCodeBy(ebms3Error.getErrorCode()), ebms3Error.getErrorDetail(), ebms3Error.getRefToMessageInError(), null);
                ebMS3Ex.setMshRole(MSHRole.SENDING);
                throw ebMS3Ex;
            }
        }

        return ResponseStatus.WARNING;
    }


    public enum ResponseStatus {
        OK, WARNING, UNMARSHALL_ERROR
    }

}
