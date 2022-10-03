package eu.domibus.core.message;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.message.UserMessageException;
import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.*;
import eu.domibus.api.pmode.PModeService;
import eu.domibus.api.pmode.PModeServiceHelper;
import eu.domibus.api.pmode.domain.LegConfiguration;
import eu.domibus.api.usermessage.UserMessageRestoreService;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.pull.PullMessageService;
import eu.domibus.core.plugin.notification.BackendNotificationService;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This service class is responsible for the restore of failed messages.
 *
 * @author Soumya
 * @since 4.2.2
 */

@Service
public class UserMessageDefaultRestoreService implements UserMessageRestoreService {

    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageDefaultRestoreService.class);

    private MessageExchangeService messageExchangeService;

    private BackendNotificationService backendNotificationService;

    private UserMessageLogDao userMessageLogDao;

    private PModeProvider pModeProvider;

    private PullMessageService pullMessageService;

    private PModeService pModeService;

    private PModeServiceHelper pModeServiceHelper;

    private UserMessageDefaultService userMessageService;

    private UserMessageDao userMessageDao;

    private AuditService auditService;

    public UserMessageDefaultRestoreService(MessageExchangeService messageExchangeService, BackendNotificationService backendNotificationService, UserMessageLogDao userMessageLogDao, PModeProvider pModeProvider, PullMessageService pullMessageService, PModeService pModeService, PModeServiceHelper pModeServiceHelper, UserMessageDefaultService userMessageService, UserMessageDao userMessageDao, AuditService auditService) {
        this.messageExchangeService = messageExchangeService;
        this.backendNotificationService = backendNotificationService;
        this.userMessageLogDao = userMessageLogDao;
        this.pModeProvider = pModeProvider;
        this.pullMessageService = pullMessageService;
        this.pModeService = pModeService;
        this.pModeServiceHelper = pModeServiceHelper;
        this.userMessageService = userMessageService;
        this.userMessageDao = userMessageDao;
        this.auditService = auditService;
    }


    @Transactional
    @Override
    public void restoreFailedMessage(String messageId) {
        LOG.info("Restoring message [{}]-[{}]", messageId, MSHRole.SENDING);

        final UserMessageLog userMessageLog = userMessageService.getFailedMessage(messageId);

        if (MessageStatus.DELETED == userMessageLog.getMessageStatus()) {
            throw new UserMessageException(DomibusCoreErrorCode.DOM_001, "Could not restore message [" + messageId + "]. Message status is [" + MessageStatus.DELETED + "]");
        }

        UserMessage userMessage = userMessageDao.findByEntityId(userMessageLog.getEntityId());

        final MessageStatusEntity newMessageStatus = messageExchangeService.retrieveMessageRestoreStatus(messageId, userMessage.getMshRole().getRole());
        backendNotificationService.notifyOfMessageStatusChange(userMessage, userMessageLog, newMessageStatus.getMessageStatus(), new Timestamp(System.currentTimeMillis()));
        userMessageLog.setMessageStatus(newMessageStatus);
        final Date currentDate = new Date();
        userMessageLog.setRestored(currentDate);
        userMessageLog.setFailed(null);
        userMessageLog.setNextAttempt(currentDate);

        Integer newMaxAttempts = computeNewMaxAttempts(userMessageLog);
        LOG.debug("Increasing the max attempts for message [{}] from [{}] to [{}]", messageId, userMessageLog.getSendAttemptsMax(), newMaxAttempts);
        userMessageLog.setSendAttemptsMax(newMaxAttempts);

        userMessageLogDao.update(userMessageLog);

        if (MessageStatus.READY_TO_PULL != newMessageStatus.getMessageStatus()) {
            userMessageService.scheduleSending(userMessage, userMessageLog);
        } else {
            try {
                MessageExchangeConfiguration userMessageExchangeConfiguration = pModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, true);
                String pModeKey = userMessageExchangeConfiguration.getPmodeKey();
                LOG.debug("[restoreFailedMessage]:Message:[{}] add lock", userMessage.getMessageId());
                pullMessageService.addPullMessageLock(userMessage, userMessageLog);
            } catch (EbMS3Exception ebms3Ex) {
                LOG.error("Error restoring user message to ready to pull[" + userMessage.getMessageId() + "]", ebms3Ex);
            }
        }
    }

    protected Integer getMaxAttemptsConfiguration(final Long messageEntityId) {
        final LegConfiguration legConfiguration = pModeService.getLegConfiguration(messageEntityId);
        Integer result = 1;
        if (legConfiguration == null) {
            LOG.warn("Could not get the leg configuration for message with entity id [{}]. Using the default maxAttempts configuration [{}]", messageEntityId, result);
        } else {
            result = pModeServiceHelper.getMaxAttempts(legConfiguration);
        }
        return result;
    }

    protected Integer computeNewMaxAttempts(final UserMessageLog userMessageLog) {
        Integer maxAttemptsConfiguration = getMaxAttemptsConfiguration(userMessageLog.getEntityId());
        // always increase maxAttempts (even when not reached by sendAttempts)
        return userMessageLog.getSendAttemptsMax() + maxAttemptsConfiguration + 1; // max retries plus initial reattempt
    }

    @Transactional
    @Override
    public void resendFailedOrSendEnqueuedMessage(String messageId) {
        final UserMessageLog userMessageLog = userMessageLogDao.findByMessageId(messageId, MSHRole.SENDING);
        if (userMessageLog == null) {
            throw new MessageNotFoundException(messageId);
        }
        if (MessageStatus.SEND_ENQUEUED == userMessageLog.getMessageStatus()) {
            userMessageService.sendEnqueuedMessage(messageId);
        } else {
            restoreFailedMessage(messageId);
        }

        auditService.addMessageResentAudit(messageId);
    }

    @Transactional
    @Override
    public List<String> restoreFailedMessagesDuringPeriod(Long failedStartDate, Long failedEndDate, String finalRecipient, String originalUser) {
        final List<String> failedMessages = userMessageLogDao.findFailedMessages(finalRecipient, originalUser, failedStartDate, failedEndDate);
        if (CollectionUtils.isEmpty(failedMessages)) {
            return null;
        }
        LOG.debug("Found failed messages [{}] using start ID_PK date-hour [{}], end ID_PK date-hour [{}] and final recipient [{}]", failedMessages, failedStartDate, failedEndDate, finalRecipient);

        final List<String> restoredMessages = new ArrayList<>();
        for (String messageId : failedMessages) {
            try {
                restoreFailedMessage(messageId);
                restoredMessages.add(messageId);
            } catch (Exception e) {
                LOG.error("Failed to restore message [" + messageId + "]", e);
            }
        }

        LOG.debug("Restored messages [{}] using start ID_PK date-hour [{}], end ID_PK date-hour [{}] and final recipient [{}]", restoredMessages, failedStartDate, failedEndDate, finalRecipient);

        return restoredMessages;
    }

    @Transactional
    @Override
    public List<String> batchRestoreFailedMessagesDuringPeriod(List<String> messageIds, Long failedStartDate, Long failedEndDate, String finalRecipient, String originalUser) {
        if (CollectionUtils.isEmpty(messageIds)) {
            restoreFailedMessagesDuringPeriod(failedStartDate, failedEndDate, finalRecipient, originalUser);
        }
        final List<String> restoredMessages = new ArrayList<>();
        for (String messageId : messageIds) {
            LOG.debug("Message Id's selected to detele as batch [{}]", messageId);
            try {
                restoreFailedMessage(messageId);
                restoredMessages.add(messageId);
            } catch (Exception e) {
                LOG.error("Failed to restore message [" + messageId + "]", e);
            }
        }

        LOG.debug("Restored messages [{}] using start ID_PK date-hour [{}], end ID_PK date-hour [{}] and final recipient [{}]", restoredMessages, failedStartDate, failedEndDate, finalRecipient);

        return restoredMessages;
    }
}
