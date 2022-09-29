package eu.domibus.common;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This event is used to transfer information on the message send success event, from the core to the plugins implementing PUSH methods.
 *
 * @author idragusa
 * @since 4.2
 */
public class MessageSendSuccessEvent implements Serializable, MessageEvent {

    private static final long serialVersionUID = 1L;
    protected String messageId;
    protected Map<String, String> properties = new HashMap<>(); //NOSONAR
    protected Long messageEntityId;

    public MessageSendSuccessEvent(){}

    public MessageSendSuccessEvent(final Long messageEntityId, String messageId, Map<String, String> properties) {
        this.messageEntityId = messageEntityId;
        this.messageId = messageId;
        this.properties = properties;
    }

    @Override
    public Long getMessageEntityId() {
        return messageEntityId;
    }

    public void setMessageEntityId(Long messageEntityId) {
        this.messageEntityId = messageEntityId;
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    @Override
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> getProps() {
        return properties;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("messageId", messageId)
                .toString();
    }
}
