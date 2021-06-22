package eu.domibus.plugin.ws.backend;

import eu.domibus.common.MessageStatus;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * @author François Gautier
 * @since 5.0
 */
@Entity
@Table(name = "WS_PLUGIN_TB_BACKEND_MSG_LOG")
@NamedQuery(name = "WSBackendMessageLogEntity.findByMessageId",
        query = "select wsBackendMessageLogEntity " +
                "from WSBackendMessageLogEntity wsBackendMessageLogEntity " +
                "where wsBackendMessageLogEntity.messageId=:MESSAGE_ID")
@NamedQuery(name = "WSBackendMessageLogEntity.findRetryMessages",
        query = "select backendMessage " +
                "from WSBackendMessageLogEntity backendMessage " +
                "where backendMessage.backendMessageStatus = :BACKEND_MESSAGE_STATUS " +
                "and backendMessage.nextAttempt < :CURRENT_TIMESTAMP " +
                "and 1 <= backendMessage.sendAttempts " +
                "and backendMessage.sendAttempts <= backendMessage.sendAttemptsMax " +
                "and (backendMessage.scheduled is null or backendMessage.scheduled=false)")
public class WSBackendMessageLogEntity {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSBackendMessageLogEntity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator =  "DOMIBUS_SCALABLE_SEQUENCE")
    @GenericGenerator(
            name = "DOMIBUS_SCALABLE_SEQUENCE",
            strategy = "eu.domibus.api.model.DatePrefixedSequenceIdGenerator",
            parameters = {@org.hibernate.annotations.Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "50")})
    @Column(name = "ID_PK")
    private long entityId;

    @Column(name = "CREATION_TIME", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationTime;

    @Column(name = "MODIFICATION_TIME")
    @Temporal(TemporalType.TIMESTAMP)
    private Date modificationTime;

    @Column(name = "CREATED_BY", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "MODIFIED_BY")
    private String modifiedBy;

    @Column(name = "MESSAGE_ID", nullable = false)
    private String messageId;

    @Column(name = "FINAL_RECIPIENT")
    private String finalRecipient;

    @Column(name = "ORIGINAL_SENDER")
    private String originalSender;

    @Column(name = "BACKEND_MESSAGE_STATUS")
    @Enumerated(EnumType.STRING)
    private WSBackendMessageStatus backendMessageStatus;

    @Column(name = "MESSAGE_STATUS")
    @Enumerated(EnumType.STRING)
    private MessageStatus messageStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "BACKEND_MESSAGE_TYPE")
    private WSBackendMessageType type;

    @Column(name = "RULE_NAME")
    private String ruleName;

    @Column(name = "SENT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date sent;

    @Column(name = "FAILED")
    @Temporal(TemporalType.TIMESTAMP)
    private Date failed;

    @Column(name = "SEND_ATTEMPTS")
    private int sendAttempts;

    @Column(name = "SEND_ATTEMPTS_MAX")
    private int sendAttemptsMax;

    @Column(name = "NEXT_ATTEMPT")
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextAttempt;

    @Column(name = "SCHEDULED")
    protected Boolean scheduled;

    public WSBackendMessageLogEntity() {
        String user = LOG.getMDC(DomibusLogger.MDC_USER);
        if (StringUtils.isBlank(user)) {
            user = "wsplugin_default";
        }
        setCreatedBy(user);
        setSent(new Date());
        setCreationTime(new Date());
        setModificationTime(new Date());
        setSendAttempts(0);
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFinalRecipient() {
        return finalRecipient;
    }

    public void setFinalRecipient(String finalRecipient) {
        this.finalRecipient = finalRecipient;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public WSBackendMessageLogEntity setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
        return this;
    }

    public WSBackendMessageStatus getBackendMessageStatus() {
        return backendMessageStatus;
    }

    public void setBackendMessageStatus(WSBackendMessageStatus backendMessageStatus) {
        this.backendMessageStatus = backendMessageStatus;
    }

    public MessageStatus getMessageStatus() {
        return messageStatus;
    }

    public void setMessageStatus(MessageStatus messageStatus) {
        this.messageStatus = messageStatus;
    }

    public WSBackendMessageType getType() {
        return type;
    }

    public void setType(WSBackendMessageType type) {
        this.type = type;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String endpoint) {
        this.ruleName = endpoint;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Date getFailed() {
        return failed;
    }

    public void setFailed(Date failed) {
        this.failed = failed;
    }

    public int getSendAttempts() {
        return sendAttempts;
    }

    public void setSendAttempts(int sendAttempts) {
        this.sendAttempts = sendAttempts;
    }

    public int getSendAttemptsMax() {
        return sendAttemptsMax;
    }

    public void setSendAttemptsMax(int sendAttemptsMax) {
        this.sendAttemptsMax = sendAttemptsMax;
    }

    public Date getNextAttempt() {
        return nextAttempt;
    }

    public void setNextAttempt(Date nextAttempt) {
        this.nextAttempt = nextAttempt;
    }

    public Boolean getScheduled() {
        return scheduled;
    }

    public void setScheduled(Boolean scheduled) {
        this.scheduled = scheduled;
    }

}
