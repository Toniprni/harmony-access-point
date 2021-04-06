package eu.domibus.api.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Entity
@Table(name = "TB_D_MESSAGE_STATUS")
@NamedQueries({
        @NamedQuery(name = "MessageStatusEntity.findByStatus", query = "select ms from MessageStatusEntity ms where ms.messageStatus=:MESSAGE_STATUS"),
})
public class MessageStatusEntity extends AbstractBaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", unique = true)
    protected MessageStatus messageStatus;

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        MessageStatusEntity that = (MessageStatusEntity) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(messageStatus, that.messageStatus)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(messageStatus)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("messageStatus", messageStatus)
                .toString();
    }
}
