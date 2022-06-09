package eu.domibus.core.message.splitandjoin;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.message.UserMessageDao;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jws.WebMethod;
import javax.jws.WebResult;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.*;
import javax.xml.ws.soap.SOAPBinding;
import java.io.File;


/**
 * Local endpoint used to generate the multi mimepart message for SplitAndJoin
 */
@WebServiceProvider(portName = "local-msh-dispatch", serviceName = "local-msh-dispatch-service")
@ServiceMode(Service.Mode.MESSAGE)
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class MSHSourceMessageWebservice implements Provider<SOAPMessage> {

    private static final IDomibusLogger LOG = DomibusLoggerFactory.getLogger(MSHSourceMessageWebservice.class);

    public static final String SOURCE_MESSAGE_FILE = "sourceMessageFile";

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    protected SplitAndJoinService splitAndJoinService;

    @Autowired
    protected DomainTaskExecutor domainTaskExecutor;

    @Autowired
    protected SoapUtil soapUtil;

    @Autowired
    protected XMLUtil xmlUtil;

    @Autowired
    protected Ebms3Converter ebms3Converter;

    @Autowired
    protected UserMessageDao userMessageDao;

    @WebMethod
    @WebResult(name = "soapMessageResult")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public SOAPMessage invoke(final SOAPMessage request) {
        LOG.debug("Processing SourceMessage request");

        final String domain = LOG.getMDC(DomainContextProvider.HEADER_DOMIBUS_DOMAIN);
        domainContextProvider.setCurrentDomain(domain);
        final Domain currentDomain = domainContextProvider.getCurrentDomain();

        final String contentTypeString = LOG.getMDC(Message.CONTENT_TYPE);
        final boolean compression = Boolean.valueOf(LOG.getMDC(MSHDispatcher.HEADER_DOMIBUS_SPLITTING_COMPRESSION));
        final String sourceMessageFileName = LOG.getMDC(MSHSourceMessageWebservice.SOURCE_MESSAGE_FILE);

        LOG.debug("Parsing the SourceMessage from file [{}]", sourceMessageFileName);

        SOAPMessage userMessageRequest = null;
        UserMessage userMessage = null;
        try {
            userMessageRequest = splitAndJoinService.getUserMessage(new File(sourceMessageFileName), contentTypeString);
            Ebms3Messaging ebms3Messaging = messageUtil.getMessaging(userMessageRequest);
            userMessage = ebms3Converter.convertFromEbms3(ebms3Messaging.getUserMessage());
        } catch (Exception e) {
            LOG.error("Error getting the Messaging object from the SOAPMessage", e);
            throw new WebServiceException(e);
        }
        LOG.debug("Finished parsing the SourceMessage from file [{}]", sourceMessageFileName);

        SOAPMessage finalUserMessageRequest = userMessageRequest;

        //required by lambda expression
        final UserMessage finalUserMessage = userMessage;
        domainTaskExecutor.submitLongRunningTask(
                () -> splitAndJoinService.createUserFragmentsFromSourceFile(sourceMessageFileName, finalUserMessageRequest, finalUserMessage, contentTypeString, compression),
                () -> splitAndJoinService.setSourceMessageAsFailed(finalUserMessage),
                currentDomain);

        try {
            SOAPMessage responseMessage = xmlUtil.getMessageFactorySoap12().createMessage();
            responseMessage.saveChanges();

            LOG.debug("Finished processing SourceMessage request");
            return responseMessage;
        } catch (Exception e) {
            throw new WebServiceException(e);
        }
    }
}
