package eu.domibus.common;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This event is used to transfer information on the message status changed event, from the core to the plugins implementing PUSH methods.
 *
 * @author Cosmin Baciu
 * @since 3.2.2
 */
public class MessageStatusChangeEvent implements Serializable, MessageEvent {

    protected String messageId;
    protected MessageStatus fromStatus;
    protected MessageStatus toStatus;
    protected Timestamp changeTimestamp;
    protected final Map<String, String> properties;
    protected Long messageEntityId;

    public MessageStatusChangeEvent() {
        properties = new HashMap<>();
    }

    public MessageStatusChangeEvent(Map<String, String> properties) {
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

    public MessageStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(MessageStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public MessageStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(MessageStatus toStatus) {
        this.toStatus = toStatus;
    }

    public Timestamp getChangeTimestamp() {
        return changeTimestamp;
    }

    public void setChangeTimestamp(Timestamp changeTimestamp) {
        this.changeTimestamp = changeTimestamp;
    }

    @Override
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public Map<String, String> getProps() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("messageId", messageId)
                .append("fromStatus", fromStatus)
                .append("toStatus", toStatus)
                .append("changeTimestamp", changeTimestamp)
                .toString();
    }
}
