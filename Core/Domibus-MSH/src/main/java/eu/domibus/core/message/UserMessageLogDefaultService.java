package eu.domibus.core.message;

import eu.domibus.api.model.*;
import eu.domibus.api.usermessage.UserMessageLogService;
import eu.domibus.core.alerts.configuration.connectionMonitpring.ConnectionMonitoringModuleConfiguration;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.EventProperties;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.core.message.dictionary.NotificationStatusDao;
import eu.domibus.core.message.signal.SignalMessageLogDao;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static eu.domibus.logging.DomibusLogger.MDC_MESSAGE_ENTITY_ID;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
public class UserMessageLogDefaultService implements UserMessageLogService {

    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageLogDefaultService.class);

    @Autowired
    protected UserMessageLogDao userMessageLogDao;

    @Autowired
    protected SignalMessageLogDao signalMessageLogDao;

    @Autowired
    protected BackendNotificationService backendNotificationService;

    @Autowired
    protected MessageStatusDao messageStatusDao;

    @Autowired
    protected MshRoleDao mshRoleDao;

    @Autowired
    protected NotificationStatusDao notificationStatusDao;

//    @Autowired
//    protected ConnectionMonitoringConfigurationManager connectionMonitoringConfigurationManager;

    @Autowired
    protected EventService eventService;

    public UserMessageLog findById(Long entityId) {
        return userMessageLogDao.findById(entityId);
    }

    private UserMessageLog createUserMessageLog(UserMessage userMessage, String messageStatus, String notificationStatus, String mshRole, Integer maxAttempts, String backendName) {
        UserMessageLog userMessageLog = new UserMessageLog();
        userMessageLog.setUserMessage(userMessage);
        final MessageStatusEntity messageStatusEntity = messageStatusDao.findOrCreate(MessageStatus.valueOf(messageStatus));
        userMessageLog.setMessageStatus(messageStatusEntity);

        final MSHRoleEntity mshRoleEntity = mshRoleDao.findOrCreate(MSHRole.valueOf(mshRole));
        userMessageLog.setMshRole(mshRoleEntity);

        final NotificationStatusEntity notificationStatusEntity = notificationStatusDao.findOrCreate(NotificationStatus.valueOf(notificationStatus));
        userMessageLog.setNotificationStatus(notificationStatusEntity);

        userMessageLog.setSendAttemptsMax(maxAttempts);
        userMessageLog.setBackend(backendName);

        return userMessageLog;
    }

    @Transactional
    public UserMessageLog save(UserMessage userMessage, String messageStatus, String notificationStatus, String mshRole, Integer maxAttempts, String backendName) {
        final MessageStatus status = MessageStatus.valueOf(messageStatus);
        // Builds the user message log
        final UserMessageLog userMessageLog = createUserMessageLog(userMessage, messageStatus, notificationStatus, mshRole, maxAttempts, backendName);
        userMessageLog.setUserMessage(userMessage);

        if (!userMessage.isTestMessage()) {
            backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, status, new Timestamp(System.currentTimeMillis()));
        }
        userMessageLogDao.create(userMessageLog);
        LOG.putMDC(MDC_MESSAGE_ENTITY_ID, String.valueOf(userMessage.getEntityId()));

        return userMessageLog;
    }

    protected void updateUserMessageStatus(final UserMessage userMessage, final UserMessageLog messageLog, final MessageStatus newStatus) {
        LOG.debug("Updating message status to [{}]", newStatus);

        if (!userMessage.isTestMessage()) {
            backendNotificationService.notifyOfMessageStatusChange(userMessage, messageLog, newStatus, new Timestamp(System.currentTimeMillis()));
        } else {
//            final ConnectionMonitoringModuleConfiguration connMonitorConfig = connectionMonitoringConfigurationManager.getConfiguration();
            final ConnectionMonitoringModuleConfiguration connMonitorConfig = (ConnectionMonitoringModuleConfiguration) AlertType.CONNECTION_MONITORING_FAILED.getConfiguration();
            String fromParty = userMessage.getPartyInfo().getFromParty();
            String toParty = userMessage.getPartyInfo().getToParty();
            if (connMonitorConfig.shouldGenerateAlert(newStatus, toParty)) {
                eventService.enqueueEvent(EventType.CONNECTION_MONITORING_FAILED, toParty, connMonitorConfig.getFrequency(),
                        new EventProperties(userMessage.getMessageId(), messageLog.getMshRole().getRole().name(), messageLog.getMessageStatus().name(), fromParty, toParty));
//                eventService.enqueueConnectionMonitoringEvent(userMessage.getMessageId(), messageLog.getMshRole().getRole(),
//                        messageLog.getMessageStatus(), fromParty, toParty, connMonitorConfig.getFrequency());
            }
        }
        userMessageLogDao.setMessageStatus(messageLog, newStatus);
    }

    public void setMessageAsDeleted(final UserMessage userMessage, final UserMessageLog messageLog) {
        updateUserMessageStatus(userMessage, messageLog, MessageStatus.DELETED);
    }

    /**
     * Find the {@link SignalMessageLog} and set to {@link MessageStatus#DELETED}
     */
    public boolean setSignalMessageAsDeleted(final SignalMessage signalMessage) {

        if (signalMessage == null) {
            LOG.debug("Could not delete SignalMessage: received SignalMessage is null ");
            return false;
        }
        if (isBlank(signalMessage.getSignalMessageId())) {
            LOG.debug("Could not delete SignalMessage: received messageId is empty [{}",
                    signalMessage
            );
            return false;
        }

        final SignalMessageLog signalMessageLog = signalMessageLogDao.read(signalMessage.getEntityId());
        signalMessageLog.setDeleted(new Date());
        signalMessageLog.setMessageStatus(messageStatusDao.findOrCreate(MessageStatus.DELETED));
        LOG.debug("SignalMessage [{}] was set as DELETED.", signalMessage.getSignalMessageId());
        return true;
    }

    public void setMessageAsDownloaded(UserMessage userMessage, UserMessageLog userMessageLog) {
        updateUserMessageStatus(userMessage, userMessageLog, MessageStatus.DOWNLOADED);
    }

    public void setMessageAsAcknowledged(UserMessage userMessage, UserMessageLog userMessageLog) {
        updateUserMessageStatus(userMessage, userMessageLog, MessageStatus.ACKNOWLEDGED);
    }

    public void setMessageAsAckWithWarnings(UserMessage userMessage, UserMessageLog userMessageLog) {
        updateUserMessageStatus(userMessage, userMessageLog, MessageStatus.ACKNOWLEDGED_WITH_WARNING);
    }

    public void setMessageAsSendFailure(UserMessage userMessage, UserMessageLog userMessageLog) {
        updateUserMessageStatus(userMessage, userMessageLog, MessageStatus.SEND_FAILURE);
    }

    @Override
    public MessageStatus getMessageStatus(String messageId, MSHRole mshRole) {
        return userMessageLogDao.getMessageStatus(messageId, mshRole);
    }

    @Override
    public MessageStatus getMessageStatus(final Long messageEntityId) {
        return userMessageLogDao.getMessageStatus(messageEntityId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateStatusToArchived(List<Long> entityIds) {
        userMessageLogDao.updateArchived(entityIds);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateStatusToExported(List<Long> entityIds) {
        userMessageLogDao.updateExported(entityIds);
    }
}
