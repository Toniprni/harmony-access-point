package eu.domibus.core.ebms3.receiver.leg;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.model.Messaging;
import org.apache.cxf.binding.soap.SoapMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Thomas Dussart
 * @since 3.3
 */

@Component
@Qualifier("userMessageLegConfigurationFactory")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UserMessageLegConfigurationFactory extends AbstractMessageLegConfigurationFactory {

    @Override
    protected LegConfigurationExtractor getConfiguration(SoapMessage soapMessage, Ebms3Messaging messaging) {
        LegConfigurationExtractor legConfigurationExtractor = null;
        if (messaging.getUserMessage() != null) {
            legConfigurationExtractor = new UserMessageLegConfigurationExtractor(soapMessage, messaging);
        }
        return legConfigurationExtractor;
    }
}
