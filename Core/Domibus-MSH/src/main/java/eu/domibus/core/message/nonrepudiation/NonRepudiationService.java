package eu.domibus.core.message.nonrepudiation;

import eu.domibus.api.model.SignalMessage;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.model.UserMessageRaw;

import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Cosmin Baciu
 * @author Ion Perpegel
 * @since 3.3
 */

public interface NonRepudiationService {

    void saveRawEnvelope(String rawXMLMessage, UserMessage userMessage);

    UserMessageRaw createUserMessageRaw(SOAPMessage request) throws TransformerException;

    void saveRequest(SOAPMessage request, UserMessage userMessage);

    void saveResponse(SOAPMessage response, Long userMessageEntityId);

    /**
     * Retrieves the user message envelope xml
     *
     * @param userMessageId user message id
     * @return a string representing the envelope in xml format
     */
    String getUserMessageEnvelope(String userMessageId);

    /**
     * Retrieves the signal message envelope xml corresponding to the user message with the specified id
     *
     * @param userMessageId user message id
     * @return a string representing the envelope in xml format
     */
    String getSignalMessageEnvelope(String userMessageId);

    /**
     * Retrieves the user and signal message envelopes
     *
     * @param userMessageId user message id
     * @return a map representing the envelopes in byte array format
     */
    Map<String, InputStream> getMessageEnvelopes(String userMessageId);
}
