package eu.domibus.core.message.reliability;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3SignalMessage;
import eu.domibus.api.ebms3.model.Ebms3UserMessage;
import eu.domibus.api.ebms3.model.ObjectFactory;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Reliability;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.sender.ResponseResult;
import eu.domibus.core.error.ErrorLogService;
import eu.domibus.core.message.nonrepudiation.NonRepudiationChecker;
import eu.domibus.core.message.nonrepudiation.NonRepudiationConstants;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.apache.wss4j.dom.WSConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author Christian Koch, Stefan Mueller
 */
@Service
public class ReliabilityChecker {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReliabilityChecker.class);

    @Autowired
    @Qualifier("jaxbContextEBMS")
    protected JAXBContext jaxbContext;

    @Autowired
    protected NonRepudiationChecker nonRepudiationChecker;

    @Autowired
    protected PModeProvider pModeProvider;

    @Autowired
    protected ErrorLogService errorLogService;

    @Autowired
    protected ReliabilityMatcher pushMatcher;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    protected SoapUtil soapUtil;

    @Autowired
    protected XMLUtil xmlUtil;

    @Transactional(rollbackFor = EbMS3Exception.class)
    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final ResponseResult responseResult, final Reliability reliability) throws EbMS3Exception {
        return checkReliability(request, response, responseResult, reliability, pushMatcher);
    }

    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final ResponseResult responseResult, final LegConfiguration legConfiguration) throws EbMS3Exception {
        return check(request, response, responseResult, legConfiguration, pushMatcher);
    }


    public CheckResult check(final SOAPMessage request, final SOAPMessage response, final ResponseResult responseResult, final LegConfiguration legConfiguration, final ReliabilityMatcher matcher) throws EbMS3Exception {
        return checkReliability(request, response, responseResult, legConfiguration.getReliability(), matcher);
    }

    protected CheckResult checkReliability(final SOAPMessage request, final SOAPMessage response, final ResponseResult responseResult, Reliability reliability, final ReliabilityMatcher matcher) throws EbMS3Exception {
        String messageId = null;

        if (matcher.matchReliableCallBack(reliability)) {
            LOG.debug("Reply pattern is waiting for callback, setting message status to WAITING_FOR_CALLBACK.");
            return CheckResult.WAITING_FOR_CALLBACK;
        }

        if (matcher.matchReliableReceipt(reliability)) {
            LOG.debug("Checking reliability for outgoing message");
            final Ebms3Messaging ebms3Messaging = responseResult.getResponseMessaging();
            final Ebms3SignalMessage ebms3SignalMessage = ebms3Messaging.getSignalMessage();

            //ReceiptionAwareness or NRR found but not expected? report if configuration=true //TODO: make configurable in domibus.properties

            //SignalMessage with Receipt expected
            messageId = getMessageId(ebms3SignalMessage);
            if (ebms3SignalMessage.getReceipt() != null && ebms3SignalMessage.getReceipt().getAny().size() == 1) {

                final String contentOfReceiptString = ebms3SignalMessage.getReceipt().getAny().get(0);

                try {
                    if (!reliability.isNonRepudiation()) {
                        XMLInputFactory inputFactory = xmlUtil.getXmlInputFactory();
                        StreamSource source = new StreamSource(new ByteArrayInputStream(contentOfReceiptString.getBytes()));
                        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(source);
                        final Ebms3UserMessage ebms3UserMessage = this.jaxbContext.createUnmarshaller().unmarshal(streamReader, Ebms3UserMessage.class).getValue();

                        Node node = (Node) request.getSOAPHeader().getChildElements(ObjectFactory._Messaging_QNAME).next();
                        XMLStreamReader reader = xmlUtil.getXmlStreamReaderFromNode(node);
                        final Ebms3UserMessage ebms3UserMessageInRequest = this.jaxbContext.createUnmarshaller().unmarshal(reader, Ebms3Messaging.class).getValue().getUserMessage();
                        if (!ebms3UserMessage.equals(ebms3UserMessageInRequest)) {
                            ReliabilityChecker.LOG.warn("Reliability check failed, the user message in the request does not match the user message in the response.");
                            return matcher.fails();
                        }

                        return CheckResult.OK;
                    }

                    final Iterator elementIterator = response.getSOAPHeader().getChildElements(new QName(WSConstants.WSSE_NS, WSConstants.WSSE_LN));

                    if (!elementIterator.hasNext()) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_NO_SECURITY_HEADER, messageId);
                        throw EbMS3ExceptionBuilder.getInstance()
                                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                                .message("Invalid NonRepudiationInformation: No security header found")
                                .refToMessageId(messageId)
                                .mshRole(MSHRole.SENDING)
                                .signalMessageId(messageId)
                                .build();
                    }
                    final Element securityHeaderResponse = (Element) elementIterator.next();

                    if (elementIterator.hasNext()) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_MULTIPLE_SECURITY_HEADERS, messageId);
                        throw EbMS3ExceptionBuilder.getInstance()
                                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                                .message("Invalid NonRepudiationInformation: Multiple security headers found")
                                .refToMessageId(messageId)
                                .mshRole(MSHRole.SENDING)
                                .signalMessageId(messageId)
                                .build();
                    }

                    final String wsuIdOfMEssagingElement = ebms3Messaging.getOtherAttributes().get(new QName(WSConstants.WSU_NS, "Id"));

                    ReliabilityChecker.LOG.debug(wsuIdOfMEssagingElement);

                    final NodeList nodeList = securityHeaderResponse.getElementsByTagNameNS(WSConstants.SIG_NS, WSConstants.REF_LN);
                    boolean signatureFound = false;
                    for (int i = 0; i < nodeList.getLength(); i++) {
                        final Node node = nodeList.item(i);
                        if (this.compareReferenceIgnoreHashtag(node.getAttributes().getNamedItem("URI").getNodeValue(), wsuIdOfMEssagingElement)) {
                            signatureFound = true;
                            break;
                        }
                    }
                    if (!signatureFound) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_MESSAGING_NOT_SIGNED, messageId);
                        LOG.error("Response message [{}]", soapPartToString(response));
                        throw EbMS3ExceptionBuilder.getInstance()
                                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                                .message("Invalid NonRepudiationInformation: eb:Messaging not signed")
                                .refToMessageId(messageId)
                                .mshRole(MSHRole.SENDING)
                                .signalMessageId(messageId)
                                .build();
                    }

                    final List<String> referencesFromSecurityHeader = nonRepudiationChecker.getNonRepudiationDetailsFromSecurityInfoNode(request.getSOAPHeader().getElementsByTagNameNS(WSConstants.SIG_NS, WSConstants.SIG_INFO_LN).item(0));
                    final Node nonRepudiationDetailsNode = getNonRepudiationDetailsNodeFromReceipt(response, messageId);
                    final List<String> referencesFromNonRepudiationInformation = nonRepudiationChecker.getNonRepudiationDetailsFromReceipt(nonRepudiationDetailsNode);

                    if (!nonRepudiationChecker.compareUnorderedReferenceNodeLists(referencesFromSecurityHeader, referencesFromNonRepudiationInformation)) {
                        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_NOT_MATCHING_THE_MESSAGE, soapPartToString(response), soapPartToString(request));
                        throw EbMS3ExceptionBuilder.getInstance()
                                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                                .message("Invalid NonRepudiationInformation: non repudiation information and request message do not match")
                                .refToMessageId(messageId)
                                .mshRole(MSHRole.SENDING)
                                .signalMessageId(messageId)
                                .build();
                    }

                    LOG.businessInfo(DomibusMessageCode.BUS_RELIABILITY_SUCCESSFUL, messageId);
                    return CheckResult.OK;
                } catch (final JAXBException | XMLStreamException | SOAPException | TransformerException e) {
                    ReliabilityChecker.LOG.error("", e);
                }

            } else {
                LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_RECEIPT_INVALID_EMPTY, messageId);
                throw EbMS3ExceptionBuilder.getInstance()
                        .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                        .message("There is no content inside the receipt element received by the responding gateway")
                        .refToMessageId(messageId)
                        .mshRole(MSHRole.SENDING)
                        .signalMessageId(messageId)
                        .build();
            }

        }
        LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_GENERAL_ERROR, messageId);
        return matcher.fails();

    }

    /**
     * Method returns NonRepudiationDetails element or EBMS3 exception if element does not exists
     * @param response: soap as4 NonRepudiation receipt
     * @param messageId: outgoing message is
     * @return NonRepudiationDetails node
     * @throws EbMS3Exception: element NonRepudiationDetails does not exist in response
     * @throws SOAPException: error when parsing response
     */
    protected Node getNonRepudiationDetailsNodeFromReceipt(final SOAPMessage response, final String messageId) throws EbMS3Exception, SOAPException {
        final NodeList nodeList = response.getSOAPHeader().getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN);
        if (nodeList.getLength() == 0 || nodeList.item(0) == null) {
            LOG.businessError(DomibusMessageCode.BUS_RELIABILITY_INVALID_WITH_NO_SECURITY_HEADER, messageId);
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0302)
                    .message("Invalid NonRepudiationInformation: No element found")
                    .refToMessageId(messageId)
                    .mshRole(MSHRole.SENDING)
                    .signalMessageId(messageId)
                    .build();
        }
        return nodeList.item(0);
    }


    protected String soapPartToString(SOAPMessage soapMessage) {
        if(soapMessage == null) {
            return null;
        }
        try (StringWriter stringWriter = new StringWriter()) {
            Transformer transformer = xmlUtil.getTransformerFactory().newTransformer();
            transformer.transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(stringWriter));
            return stringWriter.toString();
        } catch (IOException | TransformerException e) {
            LOG.warn("Couldn't get soap part", e);
        }
        return null;
    }

    protected String getMessageId(Ebms3SignalMessage ebms3SignalMessage) {
        if(ebms3SignalMessage == null || ebms3SignalMessage.getMessageInfo() == null) {
            return null;
        }
        return ebms3SignalMessage.getMessageInfo().getMessageId();
    }

    /**
     * Compares two contentIds but ignores hashtags that were used for referencing inside a document
     *
     * @param referenceId the id with an hashtag
     * @param contentId   the id of the content to match
     * @return {@code true} in case both values are equal (ignoring the hashtag), else {@link false}
     */
    private boolean compareReferenceIgnoreHashtag(final String referenceId, final String contentId) {
        return referenceId.substring(1).equals(contentId);
    }

    public enum CheckResult {
        OK, SEND_FAIL, PULL_FAILED, WAITING_FOR_CALLBACK, ABORT
    }

    /**
     * This method is responsible for the ebMS3 error handling (creation of errorlogs and marking message as sent)
     *
     * @param exceptionToHandle the exception {@link EbMS3Exception} that needs to be handled
     * @param userMessage       the userMessage the exception belongs to
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEbms3Exception(final EbMS3Exception exceptionToHandle, final UserMessage userMessage) {
        exceptionToHandle.setRefToMessageId(userMessage.getMessageId());

        this.errorLogService.createErrorLog(exceptionToHandle, MSHRole.SENDING, userMessage);
        // The backends are notified that an error occurred in the UpdateRetryLoggingService
    }
}
