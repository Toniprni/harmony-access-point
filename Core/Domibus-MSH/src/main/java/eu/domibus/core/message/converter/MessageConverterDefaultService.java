package eu.domibus.core.message.converter;

import eu.domibus.api.ebms3.model.Ebms3Messaging;
import eu.domibus.api.ebms3.model.Ebms3UserMessage;
import eu.domibus.api.messaging.MessagingException;
import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessage;
import eu.domibus.core.ebms3.mapper.Ebms3Converter;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Created by musatmi on 11/05/2017.
 */
@Service
public class MessageConverterDefaultService implements MessageConverterService {
    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageConverterDefaultService.class);

    @Autowired
    @Qualifier("jaxbContextEBMS")
    private JAXBContext jaxbContext;

    @Autowired
    protected Ebms3Converter ebms3Converter;

    @Override
    public byte[] getAsByteArray(UserMessage userMessage, List<PartInfo> partInfoList) {
        final Ebms3UserMessage ebms3UserMessage = ebms3Converter.convertToEbms3(userMessage, partInfoList);

        Ebms3Messaging ebms3Messaging = new Ebms3Messaging();
        ebms3Messaging.setUserMessage(ebms3UserMessage);

        final Marshaller marshaller;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.marshal(ebms3Messaging, baos);
        } catch (JAXBException e) {
            throw new MessagingException("Error marshalling the message with id " + userMessage.getMessageId(), e);
        }

        return baos.toByteArray();

    }


}
