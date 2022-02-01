package eu.domibus.core.message.pull;

import eu.domibus.api.ebms3.model.Ebms3Error;
import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.common.ErrorCode;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.core.ebms3.sender.client.MSHDispatcher;
import eu.domibus.core.util.MessageUtil;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.neethi.Policy;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.soap.SOAPMessage;
import java.util.Set;

@Component
public class PullReceiptSender {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(PullReceiptSender.class);

    @Autowired
    private MSHDispatcher mshDispatcher;

    @Autowired
    protected MessageUtil messageUtil;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Transactional(propagation = Propagation.REQUIRED)
    public void sendReceipt(final SOAPMessage soapMessage, final String endpoint, final Policy policy, final LegConfiguration legConfiguration, final String pModeKey, final String messsageId, String domainCode) throws EbMS3Exception {
        domainContextProvider.setCurrentDomain(domainCode);
        LOG.trace("[sendReceipt] Message:[{}] dispatch receipt", messsageId);
        final SOAPMessage acknowledgementResult;
        try {
            acknowledgementResult = mshDispatcher.dispatch(soapMessage, endpoint, policy, legConfiguration, pModeKey);
            LOG.trace("[sendReceipt] Message:[{}] receipt result", messsageId);
            handleDispatchReceiptResult(acknowledgementResult);
        } catch (EbMS3Exception e) {
            LOG.error("Error dispatching the pull receipt for message:[{}]", messsageId, e);
            throw e;
        } finally {
            LOG.trace("[sendReceipt] ~~~ finally the end ~~~");
        }
    }

    protected void handleDispatchReceiptResult(SOAPMessage acknowledgementResult) throws EbMS3Exception {
        if (acknowledgementResult == null) {
            LOG.debug("acknowledgementResult is null, as expected. No errors were reported");
            return;
        }
        Ebms3Messaging errorMessage = messageUtil.getMessage(acknowledgementResult);
        if (errorMessage == null || errorMessage.getSignalMessage() == null) {
            LOG.debug("acknowledgementResult is not null, but it does not contain a SignalMessage with the reported errors. ");
            return;
        }
        Set<Ebms3Error> ebms3Errors = errorMessage.getSignalMessage().getError();
        if (ebms3Errors != null && !ebms3Errors.isEmpty()) {
            Ebms3Error ebms3Error = ebms3Errors.iterator().next();
            LOG.error("An error occured when sending receipt:error code:[{}], description:[{}]:[{}]", ebms3Error.getErrorCode(), ebms3Error.getShortDescription(), ebms3Error.getErrorDetail());
            throw EbMS3ExceptionBuilder.getInstance()
                    .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.findErrorCodeBy(ebms3Error.getErrorCode()))
                    .message(ebms3Error.getErrorDetail())
                    .refToMessageId(ebms3Error.getRefToMessageInError())
                    .mshRole(MSHRole.RECEIVING)
                    .build();
        }
    }
}
