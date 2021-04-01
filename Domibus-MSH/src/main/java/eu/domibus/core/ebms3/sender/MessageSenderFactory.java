package eu.domibus.core.ebms3.sender;

import eu.domibus.core.message.splitandjoin.MessageFragmentSender;
import eu.domibus.core.message.splitandjoin.SourceMessageSender;
import eu.domibus.api.model.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Cosmin Baciu
 * @since 4.1
 */
@Service
public class MessageSenderFactory {

    @Autowired
    UserMessageSender userMessageSender;

    @Autowired
    MessageFragmentSender messageFragmentSender;

    @Autowired
    SourceMessageSender sourceMessageSender;

    public MessageSender getMessageSender(final UserMessage userMessage) {
        if (userMessage.isSplitAndJoin()) {
            if (userMessage.isMessageFragment()) {
                return messageFragmentSender;
            } else {
                return sourceMessageSender;
            }
        } else {
            return userMessageSender;
        }
    }
}
