package eu.domibus.plugin.ws.webservice;

import eu.domibus.common.ErrorResult;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.exceptions.AuthenticationExtException;
import eu.domibus.ext.exceptions.MessageAcknowledgeExtException;
import eu.domibus.ext.services.AuthenticationExtService;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.MessageAcknowledgeExtService;
import eu.domibus.ext.services.MessageExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageNotFoundException;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.ws.connector.WSPluginImpl;
import eu.domibus.plugin.ws.exception.WSPluginException;
import eu.domibus.plugin.ws.generated.RetrieveMessageFault;
import eu.domibus.plugin.ws.generated.StatusFault;
import eu.domibus.plugin.ws.generated.SubmitMessageFault;
import eu.domibus.plugin.ws.generated.WebServicePluginInterface;
import eu.domibus.plugin.ws.generated.body.*;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.ObjectFactory;
import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.*;
import eu.domibus.plugin.ws.message.WSMessageLogEntity;
import eu.domibus.plugin.ws.message.WSMessageLogService;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.ws.BindingType;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("ValidExternallyBoundObject")
@javax.jws.WebService(
        serviceName = "WebServicePlugin",
        portName = "WEBSERVICEPLUGIN_PORT",
        targetNamespace = "http://eu.domibus.wsplugin/",
        endpointInterface = "eu.domibus.plugin.ws.generated.WebServicePluginInterface")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class WebServiceImpl implements WebServicePluginInterface {

    public static final String MESSAGE_SUBMISSION_FAILED = "Message submission failed";
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WebServiceImpl.class);

    public static final eu.domibus.plugin.ws.generated.body.ObjectFactory WEBSERVICE_OF = new eu.domibus.plugin.ws.generated.body.ObjectFactory();

    public static final ObjectFactory EBMS_OBJECT_FACTORY = new ObjectFactory();

    private static final String MIME_TYPE = "MimeType";

    private static final String MESSAGE_ID_EMPTY = "Message ID is empty";

    private static final String MESSAGE_NOT_FOUND_ID = "Message not found, id [";

    private MessageAcknowledgeExtService messageAcknowledgeExtService;

    protected WebServiceExceptionFactory webServicePluginExceptionFactory;

    protected WSMessageLogService wsMessageLogService;

    private DomainContextExtService domainContextExtService;

    protected WSPluginPropertyManager wsPluginPropertyManager;

    private AuthenticationExtService authenticationExtService;

    protected MessageExtService messageExtService;

    private WSPluginImpl wsPlugin;

    public WebServiceImpl(MessageAcknowledgeExtService messageAcknowledgeExtService,
                          WebServiceExceptionFactory webServicePluginExceptionFactory,
                          WSMessageLogService wsMessageLogService,
                          DomainContextExtService domainContextExtService,
                          WSPluginPropertyManager wsPluginPropertyManager,
                          AuthenticationExtService authenticationExtService,
                          MessageExtService messageExtService,
                          WSPluginImpl wsPlugin) {
        this.messageAcknowledgeExtService = messageAcknowledgeExtService;
        this.webServicePluginExceptionFactory = webServicePluginExceptionFactory;
        this.wsMessageLogService = wsMessageLogService;
        this.domainContextExtService = domainContextExtService;
        this.wsPluginPropertyManager = wsPluginPropertyManager;
        this.authenticationExtService = authenticationExtService;
        this.messageExtService = messageExtService;
        this.wsPlugin = wsPlugin;
    }

    /**
     * Add support for large files using DataHandler instead of byte[]
     *
     * @param submitRequest
     * @param ebMSHeaderInfo
     * @return {@link SubmitResponse} object
     * @throws SubmitMessageFault
     */
    @SuppressWarnings("ValidExternallyBoundObject")
    @Override
    @Transactional(propagation = Propagation.REQUIRED, timeout = 1200) // 20 minutes
    public SubmitResponse submitMessage(SubmitRequest submitRequest, Messaging ebMSHeaderInfo) throws SubmitMessageFault {
        LOG.debug("Received message");

        addPartInfos(submitRequest, ebMSHeaderInfo);
        if (ebMSHeaderInfo.getUserMessage().getMessageInfo() == null) {
            MessageInfo messageInfo = new MessageInfo();
            messageInfo.setTimestamp(LocalDateTime.now());
            ebMSHeaderInfo.getUserMessage().setMessageInfo(messageInfo);
        } else {
            final String submittedMessageId = ebMSHeaderInfo.getUserMessage().getMessageInfo().getMessageId();
            if (StringUtils.isNotEmpty(submittedMessageId)) {
                //if there is a submitted messageId we trim it
                LOG.debug("Submitted messageId=[{}]", submittedMessageId);
                String trimmedMessageId = messageExtService.cleanMessageIdentifier(submittedMessageId);
                ebMSHeaderInfo.getUserMessage().getMessageInfo().setMessageId(trimmedMessageId);
            }
        }

        final String messageId;
        try {
            messageId = wsPlugin.submit(ebMSHeaderInfo);
        } catch (final MessagingProcessingException mpEx) {
            LOG.error(MESSAGE_SUBMISSION_FAILED, mpEx);
            throw new SubmitMessageFault(MESSAGE_SUBMISSION_FAILED, generateFaultDetail(mpEx));
        }
        LOG.info("Received message from backend with messageID [{}]", messageId);
        final SubmitResponse response = WEBSERVICE_OF.createSubmitResponse();
        response.getMessageID().add(messageId);
        return response;
    }

    public UserMessage getUserMessage(String messageId) {
        UserMessage userMessage;
        try {
            userMessage = wsPlugin.downloadMessage(messageId, null);
        } catch (final MessageNotFoundException mnfEx) {
            throw new WSPluginException(MESSAGE_NOT_FOUND_ID + messageId + "]");
        }

        if (userMessage == null) {
            throw new WSPluginException(MESSAGE_NOT_FOUND_ID + messageId + "]");
        }
        return userMessage;
    }

    private void addPartInfos(SubmitRequest submitRequest, Messaging ebMSHeaderInfo) throws SubmitMessageFault {

        if (getPayloadInfo(ebMSHeaderInfo) == null) {
            return;
        }

        validateSubmitRequest(submitRequest);

        List<PartInfo> partInfoList = getPartInfo(ebMSHeaderInfo);
        List<ExtendedPartInfo> partInfosToAdd = new ArrayList<>();

        for (Iterator<PartInfo> i = partInfoList.iterator(); i.hasNext(); ) {
            ExtendedPartInfo extendedPartInfo = new ExtendedPartInfo(i.next());
            partInfosToAdd.add(extendedPartInfo);
            i.remove();

            initPartInfoPayLoad(submitRequest, extendedPartInfo);
        }
        partInfoList.addAll(partInfosToAdd);
    }

    private void initPartInfoPayLoad(SubmitRequest submitRequest, ExtendedPartInfo extendedPartInfo) throws SubmitMessageFault {
        boolean foundPayload = false;
        final String href = extendedPartInfo.getHref();
        LOG.debug("Looking for payload: {}", href);
        for (final LargePayloadType payload : submitRequest.getPayload()) {
            LOG.debug("comparing with payload id: " + payload.getPayloadId());
            if (StringUtils.equalsIgnoreCase(payload.getPayloadId(), href)) {
                this.copyPartProperties(payload.getContentType(), extendedPartInfo);
                extendedPartInfo.setInBody(false);
                LOG.debug("sendMessage - payload Content Type: " + payload.getContentType());
                extendedPartInfo.setPayloadDatahandler(payload.getValue());
                foundPayload = true;
                break;
            }
        }

        if (!foundPayload) {
            initPayloadInBody(submitRequest, extendedPartInfo, href);
        }
    }

    private void initPayloadInBody(SubmitRequest submitRequest, ExtendedPartInfo extendedPartInfo, String href) throws SubmitMessageFault {
        final LargePayloadType bodyload = submitRequest.getBodyload();
        if (bodyload == null) {
            // in this case the payload referenced in the partInfo was neither an external payload nor a bodyload
            throw new SubmitMessageFault("No Payload or Bodyload found for PartInfo with href: " + extendedPartInfo.getHref(), generateDefaultFaultDetail(extendedPartInfo.getHref()));
        }
        // It can only be in body load, href MAY be null!
        if (href == null && bodyload.getPayloadId() == null || href != null && StringUtils.equalsIgnoreCase(href, bodyload.getPayloadId())) {
            this.copyPartProperties(bodyload.getContentType(), extendedPartInfo);
            extendedPartInfo.setInBody(true);
            LOG.debug("sendMessage - bodyload Content Type: " + bodyload.getContentType());
            extendedPartInfo.setPayloadDatahandler(bodyload.getValue());
        } else {
            throw new SubmitMessageFault("No payload found for PartInfo with href: " + extendedPartInfo.getHref(), generateDefaultFaultDetail(extendedPartInfo.getHref()));
        }
    }

    protected void validateSubmitRequest(SubmitRequest submitRequest) throws SubmitMessageFault {
        for (final LargePayloadType payload : submitRequest.getPayload()) {
            if (StringUtils.isBlank(payload.getPayloadId())) {
                throw new SubmitMessageFault("Invalid request", generateDefaultFaultDetail("Attribute 'payloadId' of the 'payload' element must not be empty"));
            }
        }
        final LargePayloadType bodyload = submitRequest.getBodyload();
        if (bodyload != null && StringUtils.isNotBlank(bodyload.getPayloadId())) {
            throw new SubmitMessageFault("Invalid request", generateDefaultFaultDetail("Attribute 'payloadId' must not appear on element 'bodyload'"));
        }
    }

    private FaultDetail generateFaultDetail(MessagingProcessingException mpEx) {
        FaultDetail fd = WEBSERVICE_OF.createFaultDetail();
        fd.setCode(mpEx.getEbms3ErrorCode().getErrorCodeName());
        fd.setMessage(mpEx.getMessage());
        return fd;
    }

    private FaultDetail generateDefaultFaultDetail(String message) {
        FaultDetail fd = WEBSERVICE_OF.createFaultDetail();
        fd.setCode(ErrorCode.EBMS_0004.name());
        fd.setMessage(message);
        return fd;
    }

    private void copyPartProperties(final String payloadContentType, final ExtendedPartInfo partInfo) {
        final PartProperties partProperties = new PartProperties();
        Property prop;

        // add all partproperties WEBSERVICE_OF the backend message
        if (partInfo.getPartProperties() != null) {
            for (final Property property : partInfo.getPartProperties().getProperty()) {
                prop = new Property();

                prop.setName(property.getName());
                prop.setValue(property.getValue());
                partProperties.getProperty().add(prop);
            }
        }

        boolean mimeTypePropFound = false;
        for (final Property property : partProperties.getProperty()) {
            if (MIME_TYPE.equals(property.getName())) {
                mimeTypePropFound = true;
                break;
            }
        }
        // in case there was no property with name {@value Property.MIME_TYPE} and xmime:contentType attribute was set noinspection SuspiciousMethodCalls
        if (!mimeTypePropFound && payloadContentType != null) {
            prop = new Property();
            prop.setName(MIME_TYPE);
            prop.setValue(payloadContentType);
            partProperties.getProperty().add(prop);
        }
        partInfo.setPartProperties(partProperties);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 300) // 5 minutes
    public ListPendingMessagesResponse listPendingMessages(ListPendingMessagesRequest listPendingMessagesRequest) {
        DomainDTO domainDTO = domainContextExtService.getCurrentDomainSafely();
        LOG.info("ListPendingMessages for domain [{}]", domainDTO);

        final ListPendingMessagesResponse response = WEBSERVICE_OF.createListPendingMessagesResponse();
        final int intMaxPendingMessagesRetrieveCount = wsPluginPropertyManager.getKnownIntegerPropertyValue(WSPluginPropertyManager.PROP_LIST_PENDING_MESSAGES_MAXCOUNT);
        LOG.debug("maxPendingMessagesRetrieveCount [{}]", intMaxPendingMessagesRetrieveCount);

        String finalRecipient =  listPendingMessagesRequest.getFinalRecipient();
        if (!authenticationExtService.isUnsecureLoginAllowed()) {
            String originalUser = authenticationExtService.getOriginalUser();
            if (StringUtils.isNotEmpty(finalRecipient)) {
                LOG.warn("finalRecipient [{}] provided in listPendingMessagesRequest is overridden by authenticated user [{}]", finalRecipient, originalUser);
            }
            finalRecipient = originalUser;
        }
        LOG.info("Final Recipient is [{}]", finalRecipient);

        List<WSMessageLogEntity> pending = wsMessageLogService.findAllWithFilter(
                listPendingMessagesRequest.getMessageId(),
                listPendingMessagesRequest.getFromPartyId(),
                listPendingMessagesRequest.getConversationId(),
                listPendingMessagesRequest.getRefToMessageId(),
                listPendingMessagesRequest.getOriginalSender(),
                finalRecipient,
                listPendingMessagesRequest.getReceivedFrom(),
                listPendingMessagesRequest.getReceivedTo(),
                intMaxPendingMessagesRetrieveCount);

        final Collection<String> ids = pending.stream()
                .map(WSMessageLogEntity::getMessageId).collect(Collectors.toList());
        response.getMessageID().addAll(ids);
        return response;
    }

    /**
     * Add support for large files using DataHandler instead of byte[]
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 300, rollbackFor = RetrieveMessageFault.class)
    @MDCKey(value = {DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ENTITY_ID}, cleanOnStart = true)
    public void retrieveMessage(RetrieveMessageRequest retrieveMessageRequest,
                                Holder<RetrieveMessageResponse> retrieveMessageResponse,
                                Holder<Messaging> ebMSHeaderInfo) throws RetrieveMessageFault {
        UserMessage userMessage;
        boolean isMessageIdNotEmpty = StringUtils.isNotEmpty(retrieveMessageRequest.getMessageID());

        if (!isMessageIdNotEmpty) {
            LOG.error(MESSAGE_ID_EMPTY);
            throw new RetrieveMessageFault(MESSAGE_ID_EMPTY, webServicePluginExceptionFactory.createFault("MessageId is empty"));
        }

        String trimmedMessageId = messageExtService.cleanMessageIdentifier(retrieveMessageRequest.getMessageID());
        WSMessageLogEntity wsMessageLogEntity =  wsMessageLogService.findByMessageId(trimmedMessageId);
        if (wsMessageLogEntity == null) {
            LOG.businessError(DomibusMessageCode.BUS_MSG_NOT_FOUND, trimmedMessageId);
            throw new RetrieveMessageFault(MESSAGE_NOT_FOUND_ID + trimmedMessageId + "]", webServicePluginExceptionFactory.createFault("No message with id [" + trimmedMessageId + "] pending for download"));
        }

        userMessage = getUserMessage(retrieveMessageRequest, trimmedMessageId);

        // To avoid blocking errors during the Header's response validation
        if (StringUtils.isEmpty(userMessage.getCollaborationInfo().getAgreementRef().getValue())) {
            userMessage.getCollaborationInfo().setAgreementRef(null);
        }
        Messaging messaging = EBMS_OBJECT_FACTORY.createMessaging();
        messaging.setUserMessage(userMessage);
        ebMSHeaderInfo.value = messaging;
        retrieveMessageResponse.value = WEBSERVICE_OF.createRetrieveMessageResponse();

        fillInfoPartsForLargeFiles(retrieveMessageResponse, messaging);

        try {
            messageAcknowledgeExtService.acknowledgeMessageDelivered(trimmedMessageId, new Timestamp(System.currentTimeMillis()));
        } catch (AuthenticationExtException | MessageAcknowledgeExtException e) {
            //if an error occurs related to the message acknowledgement do not block the download message operation
            LOG.error("Error acknowledging message [" + retrieveMessageRequest.getMessageID() + "]", e);
        }

        // remove downloaded message from the plugin table containing the pending messages
        wsMessageLogService.delete(wsMessageLogEntity);
    }

    private UserMessage getUserMessage(RetrieveMessageRequest retrieveMessageRequest, String trimmedMessageId) throws RetrieveMessageFault {
        UserMessage userMessage;
        try {
            userMessage = wsPlugin.downloadMessage(trimmedMessageId, null);
        } catch (final MessageNotFoundException mnfEx) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(MESSAGE_NOT_FOUND_ID + retrieveMessageRequest.getMessageID() + "]", mnfEx);
            }
            LOG.error(MESSAGE_NOT_FOUND_ID + retrieveMessageRequest.getMessageID() + "]");
            throw new RetrieveMessageFault(MESSAGE_NOT_FOUND_ID + trimmedMessageId + "]", webServicePluginExceptionFactory.createDownloadMessageFault(mnfEx));
        }

        if (userMessage == null) {
            LOG.error(MESSAGE_NOT_FOUND_ID + retrieveMessageRequest.getMessageID() + "]");
            throw new RetrieveMessageFault(MESSAGE_NOT_FOUND_ID + trimmedMessageId + "]", webServicePluginExceptionFactory.createFault("UserMessage not found"));
        }
        return userMessage;
    }

    private void fillInfoPartsForLargeFiles(Holder<RetrieveMessageResponse> retrieveMessageResponse, Messaging messaging) {
        if (getPayloadInfo(messaging) == null || CollectionUtils.isEmpty(getPartInfo(messaging))) {
            LOG.info("No payload found for message [{}]", messaging.getUserMessage().getMessageInfo().getMessageId());
            return;
        }

        for (final PartInfo partInfo : getPartInfo(messaging)) {
            ExtendedPartInfo extPartInfo = (ExtendedPartInfo) partInfo;
            LargePayloadType payloadType = WEBSERVICE_OF.createLargePayloadType();
            if (extPartInfo.getPayloadDatahandler() != null) {
                LOG.debug("payloadDatahandler Content Type: [{}]", extPartInfo.getPayloadDatahandler().getContentType());
                payloadType.setValue(extPartInfo.getPayloadDatahandler());
            }
            if (extPartInfo.isInBody()) {
                retrieveMessageResponse.value.setBodyload(payloadType);
            } else {
                payloadType.setPayloadId(partInfo.getHref());
                retrieveMessageResponse.value.getPayload().add(payloadType);
            }
        }
    }

    private PayloadInfo getPayloadInfo(Messaging messaging) {
        if (messaging.getUserMessage() == null) {
            return null;
        }
        return messaging.getUserMessage().getPayloadInfo();
    }

    private List<PartInfo> getPartInfo(Messaging messaging) {
        PayloadInfo payloadInfo = getPayloadInfo(messaging);
        if (payloadInfo == null) {
            return new ArrayList<>();
        }
        return payloadInfo.getPartInfo();
    }


    @Override
    public MessageStatus getStatus(final StatusRequest statusRequest) throws StatusFault {
        boolean isMessageIdNotEmpty = StringUtils.isNotEmpty(statusRequest.getMessageID());

        if (!isMessageIdNotEmpty) {
            LOG.error(MESSAGE_ID_EMPTY);
            throw new StatusFault(MESSAGE_ID_EMPTY, webServicePluginExceptionFactory.createFault("MessageId is empty"));
        }
        String trimmedMessageId = messageExtService.cleanMessageIdentifier(statusRequest.getMessageID());
        return MessageStatus.fromValue(wsPlugin.getMessageRetriever().getStatus(trimmedMessageId).name());
    }

    @Override
    public ErrorResultImplArray getMessageErrors(final GetErrorsRequest messageErrorsRequest) {
        return transformFromErrorResults(wsPlugin.getMessageRetriever().getErrorsForMessage(messageErrorsRequest.getMessageID()));
    }

    public ErrorResultImplArray transformFromErrorResults(List<? extends ErrorResult> errors) {
        ErrorResultImplArray errorList = new ErrorResultImplArray();
        for (ErrorResult errorResult : errors) {
            ErrorResultImpl errorResultImpl = new ErrorResultImpl();
            errorResultImpl.setErrorCode(ErrorCode.fromValue(errorResult.getErrorCode().name()));
            errorResultImpl.setErrorDetail(errorResult.getErrorDetail());
            errorResultImpl.setMshRole(MshRole.fromValue(errorResult.getMshRole().name()));
            errorResultImpl.setMessageInErrorId(errorResult.getMessageInErrorId());
            LocalDateTime dateTime = LocalDateTime.now(ZoneOffset.UTC);

            if (errorResult.getNotified() != null) {
                dateTime = errorResult.getNotified().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
            }
            errorResultImpl.setNotified(dateTime);
            if (errorResult.getTimestamp() != null) {
                dateTime = errorResult.getTimestamp().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
            }
            errorResultImpl.setTimestamp(dateTime);
            errorList.getItem().add(errorResultImpl);
        }
        return errorList;
    }
}
