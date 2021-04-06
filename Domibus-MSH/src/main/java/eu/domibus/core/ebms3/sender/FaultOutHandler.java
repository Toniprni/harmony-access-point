package eu.domibus.core.ebms3.sender;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.Messaging;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.core.ebms3.ws.handler.AbstractFaultHandler;
import eu.domibus.core.error.ErrorLogDao;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.util.SoapUtil;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.Collections;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * This handler is responsible for processing of incoming ebMS3 errors as a response of an outgoing ebMS3 message.
 *
 * @author Christian Koch, Stefan Mueller
 */
@Service("faultOutHandler")
public class FaultOutHandler extends AbstractFaultHandler {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FaultOutHandler.class);

    @Autowired
    private ErrorLogDao errorLogDao;

    @Autowired
    private SoapUtil soapUtil;

    @Autowired
    protected Ebms3Converter ebms3Converter;

    @Override
    public Set<QName> getHeaders() {
        return Collections.emptySet();
    }

    @Override
    public boolean handleMessage(final SOAPMessageContext context) {
        //Do nothing as this is a fault handler
        return true;
    }


    /**
     * The {@code handleFault} method is responsible for logging of incoming ebMS3 errors
     */
    @Override
    public boolean handleFault(final SOAPMessageContext context) {

        if (context == null) {
            LOG.error("Context is null and shouldn't be");
            throw new MissingResourceException("Context is null and shouldn't be", SOAPMessageContext.class.getName(), "context");
        }

        final SOAPMessage soapMessage = context.getMessage();
        final Ebms3Messaging ebms3Messaging = this.extractMessaging(soapMessage);
        if(ebms3Messaging == null) {
            LOG.trace("Messaging header is null, error log not created");
            return true;
        }
        Messaging messaging = ebms3Converter.convertFromEbms3(ebms3Messaging);
        final String messageId = messaging.getSignalMessage().getMessageInfo().getMessageId();

        //log the raw xml Signal message
        soapUtil.logRawXmlMessageWhenEbMS3Error(soapMessage);

        //save to database
        LOG.debug("An ebMS3 error was received for message with ebMS3 messageId [{}]. Please check the database for more detailed information.", messageId);
        this.errorLogDao.create(ErrorLogEntry.parse(messaging, MSHRole.SENDING));

        return true;
    }

    @Override
    public void close(final MessageContext context) {

    }
}
