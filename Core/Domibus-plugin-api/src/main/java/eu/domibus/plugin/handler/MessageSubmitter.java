
package eu.domibus.plugin.handler;

import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.plugin.Submission;

/**
 * Implementations of this interface handles the submitting of messages from the backend to Domibus.
 *
 * @author Christian Koch, Stefan Mueller
 */
public interface MessageSubmitter {

    /**
     * Submits a message to Domibus to be processed.
     *
     * @param messageData the message to be processed
     * @param submitterName the name of the submitter
     * @throws MessagingProcessingException if the message was rejected by the Domibus MSH
     */
    String submit(Submission messageData, String submitterName) throws MessagingProcessingException;

}
