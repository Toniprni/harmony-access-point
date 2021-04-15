package eu.domibus.core.message.pull;

import mockit.integration.junit4.JMockit;
import org.junit.runner.RunWith;

/**
 * @author Soumya Chandran
 * @since 4.2
 */
@RunWith(JMockit.class)
public class PullMessageStateServiceImplTest {
   /* @Tested
    PullMessageStateServiceImpl pullMessageStateService;
    @Injectable
    protected UserMessageRawEnvelopeDao rawEnvelopeLogDao;

    @Injectable
    protected UserMessageLogDao userMessageLogDao;

    @Injectable
    protected UpdateRetryLoggingService updateRetryLoggingService;

    @Injectable
    protected BackendNotificationService backendNotificationService;

    @Injectable
    protected UIReplicationSignalService uiReplicationSignalService;

    @Injectable
    protected MessagingDao messagingDao;

    @Test
    public void expirePullMessageTest(@Injectable UserMessageLog userMessageLog) {

        final String messageId = "messageId";

        new Expectations(pullMessageStateService) {{
            userMessageLogDao.findByMessageId(messageId);
            result = userMessageLog;
        }};
        pullMessageStateService.expirePullMessage(messageId);
        Assert.assertNotNull(userMessageLog);

        new Verifications() {{
            rawEnvelopeLogDao.deleteUserMessageRawEnvelope(messageId);
            times = 1;
            pullMessageStateService.sendFailed(userMessageLog);
            times = 1;
        }};

    }

    @Test
    public void sendFailedTest(@Injectable UserMessageLog userMessageLog,
                               @Injectable UserMessage userMessage) {

        new Expectations(pullMessageStateService) {{
            messagingDao.findUserMessageByMessageId(userMessageLog.getMessageId());
            result = userMessage;
        }};
        pullMessageStateService.sendFailed(userMessageLog);
        Assert.assertNotNull(userMessage);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
            times = 1;
        }};
    }

    @Test
    public void sendFailedWithNullUserMessageTest(@Injectable UserMessageLog userMessageLog,
                                                  @Injectable UserMessage userMessage) {

        new Expectations(pullMessageStateService) {{
            messagingDao.findUserMessageByMessageId(userMessageLog.getMessageId());
            result = null;
        }};
        pullMessageStateService.sendFailed(userMessageLog);

        new Verifications() {{
            updateRetryLoggingService.messageFailed(userMessage, userMessageLog);
            times = 0;
        }};
    }

    @Test
    public void resetTest(@Injectable UserMessageLog userMessageLog,
                          @Injectable MessageStatus messageStatus,
                          @Mocked Timestamp timestamp) {
        final MessageStatus readyToPull = messageStatus.READY_TO_PULL;
        new Expectations(pullMessageStateService) {{
            userMessageLog.setMessageStatus(readyToPull);
            times = 1;
            userMessageLogDao.update(userMessageLog);
            times = 1;
            uiReplicationSignalService.messageChange(userMessageLog.getMessageId());
            times = 1;
            backendNotificationService.notifyOfMessageStatusChange(userMessageLog, MessageStatus.READY_TO_PULL, new Timestamp(anyLong));
            times = 1;
        }};
        pullMessageStateService.reset(userMessageLog);

        new VerificationsInOrder() {{
            userMessageLog.setMessageStatus(withCapture());
            userMessageLogDao.update(withCapture());
            uiReplicationSignalService.messageChange(withCapture());
            backendNotificationService.notifyOfMessageStatusChange(userMessageLog, messageStatus.READY_TO_PULL, new Timestamp(anyLong));
        }};
    }*/

}