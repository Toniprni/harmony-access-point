package eu.domibus.core.jms;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.jms.*;
import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.common.NotificationType;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.jms.spi.InternalJMSDestination;
import eu.domibus.jms.spi.InternalJMSManager;
import eu.domibus.jms.spi.InternalJmsMessage;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import eu.domibus.logging.MDCKey;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsOperations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.stereotype.Component;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.*;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_JMS_QUEUE_MAX_BROWSE_SIZE;

/**
 * @author Cosmin Baciu
 * @since 3.2
 */
@Component
public class JMSManagerImpl implements JMSManager {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(JMSManagerImpl.class);

    private static final String SELECTOR = "selector";

    /**
     * queue names to be skip from showing into GUI interface
     */
    private static final String[] SKIP_QUEUE_NAMES = {};

    /**
     * multi-tenancy mode - JMS plugin queues suffixes per domain
     */
    private static final String[] JMS_QUEUE_NAMES = {
            "domibus.backend.jms.outQueue",
            "domibus.backend.jms.replyQueue",
            "domibus.backend.jms.errorNotifyConsumer",
            "domibus.backend.jms.errorNotifyProducer"
    };

    @Autowired
    private InternalJMSManager internalJmsManager;

    @Autowired
    private JMSDestinationMapper jmsDestinationMapper;

    @Autowired(required = false)
    @Qualifier("internalDestinationResolver")
    private JndiDestinationResolver jndiDestinationResolver;

    @Autowired
    private JMSMessageMapper jmsMessageMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    @Qualifier("jsonJmsTemplate")
    private JmsTemplate jmsTemplate;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    protected AuthUtils authUtils;

    @Autowired
    private DomainService domainService;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Override
    public SortedMap<String, JMSDestination> getDestinations() {
        Map<String, InternalJMSDestination> destinations = internalJmsManager.findDestinationsGroupedByFQName();
        Map<String, InternalJMSDestination> result = new HashMap<>();
        LOG.debug("JMS Messages Source Queues are [{}]", destinations);
        for (Map.Entry<String, InternalJMSDestination> mapEntry : destinations.entrySet()) {
            final String internalQueueName = mapEntry.getValue().getName();
            if (StringUtils.indexOfAny(internalQueueName, SKIP_QUEUE_NAMES) == -1 &&
                    !jmsQueueInOtherDomain(internalQueueName)) {
                result.put(mapEntry.getKey(), mapEntry.getValue());
            }
        }

        Map<String, JMSDestination> queues = jmsDestinationMapper.convert(result);
        return sortQueues(queues);
    }

    /**
     * in case of cluster environments, we reverse the name of the queue with the cluster name so that
     * the ordering shows all logical queues grouped:
     * <p>
     * Cluster1@inQueueX
     * Cluster2@inQueueX
     * Cluster1@inQueueY
     * Cluster2@inQueueY
     * in any case, we sort them by key = logicalName
     *
     * @param destinations map of {@code <String, JMSDestination>}
     * @return Sorted map of {@code <String, JMSDestination>}
     */
    protected SortedMap<String, JMSDestination> sortQueues(Map<String, JMSDestination> destinations) {
        SortedMap<String, JMSDestination> jmsDestinations = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (destinations == null) {
            return jmsDestinations;
        }

        destinations.values().forEach(queue -> {
            int ind = queue.getName().lastIndexOf('@');
            String logicalName;
            if (ind > 0) {
                logicalName = queue.getName().substring(ind + 1) + '@' + queue.getName().substring(0, ind);
            } else {
                logicalName = queue.getName();
            }
            jmsDestinations.put(logicalName, queue);
        });

        return jmsDestinations;
    }

    @Override
    public JmsMessage getMessage(String source, String messageId) {
        InternalJmsMessage internalJmsMessage = internalJmsManager.getMessage(source, messageId);
        return jmsMessageMapper.convert(internalJmsMessage);
    }

    @Override
    public List<JmsMessage> browseMessages(JmsFilterRequest req) {
        String selector = getCompleteSelector(req.getSelector(), req.getOriginalQueue());
        List<InternalJmsMessage> messagesSPI = internalJmsManager.browseMessages(req.getSource(), req.getJmsType(), req.getFromDate(), req.getToDate(), selector);
        LOG.debug("Jms Messages browsed from the source queue [{}] with the selector [{}]", req.getSource(), selector);
        return jmsMessageMapper.convert(messagesSPI);
    }

    @Override
    public List<JmsMessage> browseClusterMessages(String source, String selector) {
        LOG.debug("browseClusterMessages using selector [{}]", selector);
        List<InternalJmsMessage> messagesSPI = internalJmsManager.browseClusterMessages(source, selector);
        return jmsMessageMapper.convert(messagesSPI);
    }

    @Override
    public void convertAndSendToQueue(final Object message, final Queue destination, final String selector) {
        MessagePostProcessor messagePostProcessor = message1 -> {
            final Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
            message1.setStringProperty(JmsMessage.PROPERTY_ORIGINAL_QUEUE, destination.getQueueName());
            //that scenario occurs when sending an event with super user... EG:Login failure with super user.
            if (currentDomain != null) {
                message1.setStringProperty(MessageConstants.DOMAIN, currentDomain.getCode());
            } else {
                LOG.debug("Sending event for super user, no current domain defined");
            }
            message1.setStringProperty(SELECTOR, selector);
            return message1;
        };
        jmsTemplate.convertAndSend(destination, message, messagePostProcessor);
    }

    @Timer(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_text")
    @Counter(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_text")
    @Override
    public void sendMessageToQueue(JmsMessage message, String destination) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.TEXT_MESSAGE);
    }

    @Timer(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_map1")
    @Counter(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_map1")
    @Override
    public void sendMapMessageToQueue(JmsMessage message, String destination, JmsOperations jmsOperations) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.MAP_MESSAGE, jmsOperations);
    }

    @Timer(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_map2")
    @Counter(clazz = JMSManagerImpl.class, value = "sendMessageToQueue_map2")
    @Override
    public void sendMapMessageToQueue(JmsMessage message, String destination) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.MAP_MESSAGE);
    }

    protected void sendMessageToQueue(JmsMessage message, String destination, InternalJmsMessage.MessageType messageType) {
        InternalJmsMessage internalJmsMessage = getInternalJmsMessage(message, destination, messageType);
        internalJmsManager.sendMessage(internalJmsMessage, destination);
    }

    protected void sendMessageToQueue(JmsMessage message, String destination, InternalJmsMessage.MessageType messageType, JmsOperations jmsOperations) {
        InternalJmsMessage internalJmsMessage = getInternalJmsMessage(message, destination, messageType);
        internalJmsManager.sendMessage(internalJmsMessage, destination, jmsOperations);
    }

    @Override
    public void sendMessageToQueue(JmsMessage message, Queue destination) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.TEXT_MESSAGE);
    }

    @Override
    public void sendMapMessageToQueue(JmsMessage message, Queue destination, JmsOperations jmsOperations) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.MAP_MESSAGE, jmsOperations);
    }

    @Override
    public void sendMapMessageToQueue(JmsMessage message, Queue destination) {
        sendMessageToQueue(message, destination, InternalJmsMessage.MessageType.MAP_MESSAGE);
    }

    protected void sendMessageToQueue(JmsMessage message, Queue destination, InternalJmsMessage.MessageType messageType) {
        addOriginalQueueToMessage(message, destination);
        sendMessageToDestination(message, destination, messageType);
    }

    protected void sendMessageToQueue(JmsMessage message, Queue destination, InternalJmsMessage.MessageType messageType, JmsOperations jmsOperations) {
        addOriginalQueueToMessage(message, destination);
        sendMessageToDestination(message, destination, messageType, jmsOperations);
    }

    protected void sendMessageToDestination(JmsMessage message, Destination destination, InternalJmsMessage.MessageType messageType, JmsOperations jmsOperations) {
        InternalJmsMessage internalJmsMessage = getInternalJmsMessage(message, messageType);
        internalJmsManager.sendMessage(internalJmsMessage, destination, jmsOperations);
    }

    protected void sendMessageToDestination(JmsMessage message, Destination destination, InternalJmsMessage.MessageType messageType) {
        InternalJmsMessage internalJmsMessage = getInternalJmsMessage(message, messageType);
        internalJmsManager.sendMessage(internalJmsMessage, destination);
    }

    protected void addOriginalQueueToMessage(JmsMessage message, Queue destination) {
        try {
            message.getProperties().put(JmsMessage.PROPERTY_ORIGINAL_QUEUE, destination.getQueueName());
        } catch (JMSException e) {
            LOG.warn("Could not add the property [" + JmsMessage.PROPERTY_ORIGINAL_QUEUE + "] on the destination", e);
        }
    }

    protected void addOriginalQueueToMessage(JmsMessage message, String destination) {
        String queueName = destination;

        try {
            if (jndiDestinationResolver != null && destination != null) {
                Destination jmsDestination = jndiDestinationResolver.resolveDestinationName(null, destination, false);
                LOG.debug("Jms destination [{}] resolved to: [{}]", destination, jmsDestination);
                if (jmsDestination instanceof Queue) {
                    queueName = ((Queue) jmsDestination).getQueueName();
                }
            }
        } catch (JMSException e) {
            LOG.warn("Could not resolve jms destination [{}]", destination, e);
        }

        LOG.debug("Adding property originalQueue [{}]", queueName);
        message.getProperties().put(JmsMessage.PROPERTY_ORIGINAL_QUEUE, queueName);
    }

    protected InternalJmsMessage getInternalJmsMessage(JmsMessage message, InternalJmsMessage.MessageType messageType) {
        final Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        if (currentDomain != null) {
            LOG.debug("Adding jms message property DOMAIN: [{}]", currentDomain.getCode());
            message.getProperties().put(MessageConstants.DOMAIN, currentDomain.getCode());
        }
        InternalJmsMessage internalJmsMessage = jmsMessageMapper.convert(message);
        internalJmsMessage.setMessageType(messageType);
        return internalJmsMessage;
    }

    protected InternalJmsMessage getInternalJmsMessage(JmsMessage message, String destination, InternalJmsMessage.MessageType messageType) {
        addOriginalQueueToMessage(message, destination);
        return getInternalJmsMessage(message, messageType);
    }

    @Override
    public void sendMessageToTopic(JmsMessage message, Topic destination) {
        sendMessageToTopic(message, destination, false);
    }

    @Override
    public void sendMessageToTopic(JmsMessage message, Topic destination, boolean excludeOrigin) {
        InternalJmsMessage internalJmsMessage = getInternalJmsMessage(message, null, InternalJmsMessage.MessageType.TEXT_MESSAGE);
        internalJmsManager.sendMessageToTopic(internalJmsMessage, destination, excludeOrigin);
    }

    @Override
    public void deleteMessages(String source, String[] messageIds) {

        if (messageIds.length == 0 || Arrays.stream(messageIds).anyMatch(StringUtils::isBlank)) {
            throw new RequestValidationException("No IDs provided for messages/action");
        }

        List<JMSMessageDomainDTO> jmsMessageDomains = getJMSMessageDomain(source, messageIds);

        int deleteMessages = internalJmsManager.deleteMessages(source, messageIds);
        if (deleteMessages == 0) {
            throw new IllegalStateException("Failed to delete messages from source [" + source + "]: " + Arrays.toString(messageIds));
        }
        if (deleteMessages != messageIds.length) {
            LOG.warn("Not all the JMS messages Ids [{}] were deleted from the source queue [{}]. " +
                    "Actual: [{}], Expected [{}]", messageIds, source, deleteMessages, messageIds.length);
        }
        LOG.debug("Jms Message Ids [{}] deleted from the source queue [{}] ", messageIds, source);
        jmsMessageDomains.forEach(jmsMessageDomainDTO -> auditService.addJmsMessageDeletedAudit(jmsMessageDomainDTO.getJmsMessageId(),
                source, jmsMessageDomainDTO.getDomainCode()));
    }

    @Override
    public void deleteAllMessages(String source) {
        LOG.debug("Starting to delete all JMS messages from the source: {}", source);
        internalJmsManager.deleteAllMessages(source);
        LOG.debug("Finish to delete all JMS messages from the source: {}", source);
    }

    @Override
    public void moveMessages(String source, String destination, String[] messageIds) {
        JMSDestination jmsDestination = getValidJmsDestination(destination);
        validateMessagesMove(source, jmsDestination, messageIds);

        List<JMSMessageDomainDTO> jmsMessageDomains = getJMSMessageDomain(source, messageIds);

        int movedMessageCount = internalJmsManager.moveMessages(source, destination, messageIds);
        if (movedMessageCount == 0) {
            throw new IllegalStateException("Failed to move messages from source [" + source + "] to destination [" + destination + "]: " + Arrays.toString(messageIds));
        }

        logAndAudit(source, destination, Arrays.asList(messageIds), movedMessageCount, jmsMessageDomains);
    }

    @Override
    public void moveAllMessages(MessagesActionRequest req) {
        String destination = req.getDestination();
        String source = req.getSource();
        String jmsType = req.getJmsType();
        Date fromDate = req.getFromDate();
        Date toDate = req.getToDate();

        JMSDestination jmsDestination = getValidJmsDestination(destination);
        validateSourceAndDestination(source, jmsDestination);


        String completeSelector = getCompleteSelector(req.getSelector(), req.getOriginalQueue());
        List<InternalJmsMessage> messagesToMove = internalJmsManager.browseMessages(source, jmsType, fromDate, toDate, completeSelector);

        List<String> messageIds = messagesToMove.stream().map(InternalJmsMessage::getId).collect(Collectors.toList());
        List<JMSMessageDomainDTO> jmsMessageDomains = getJMSMessageDomain(source, messageIds.toArray(new String[0]));

        int maxLimit = domibusPropertyProvider.getIntegerProperty(DOMIBUS_JMS_QUEUE_MAX_BROWSE_SIZE);
        if (maxLimit > 0 && messagesToMove.size() > maxLimit) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Number of messages to move exceeds the limit of " + maxLimit);
        }

        int movedMessageCount = internalJmsManager.moveAllMessages(source, jmsType, fromDate, toDate, completeSelector, destination);
        if (movedMessageCount == 0) {
            throw new MessageNotFoundException(String.format("Failed to move messages from source [%s] to destination [%s] with the selector [%s]", source, destination, completeSelector));
        }

        logAndAudit(source, destination, messageIds, movedMessageCount, jmsMessageDomains);
    }

    private void logAndAudit(String source, String destination, List<String> messageIdsToMove, int movedMessageCount, List<JMSMessageDomainDTO> jmsMessageDomains) {
        if (movedMessageCount != messageIdsToMove.size()) {
            LOG.warn("Not all the JMS message were moved from the source queue [{}] to the destination queue [{}]. " +
                    "Actual: [{}], Expected [{}]", source, destination, movedMessageCount, messageIdsToMove.size());
        }

        LOG.debug("[{}] Jms Messages Moved from the source queue [{}] to the destination queue [{}]", movedMessageCount, source, destination);
        LOG.debug("Jms Message Ids [{}] Moved from the source queue [{}] to the destination queue [{}]", messageIdsToMove, source, destination);

        jmsMessageDomains.forEach(jmsMessageDomainDTO -> auditService.addJmsMessageMovedAudit(jmsMessageDomainDTO.getJmsMessageId(),
                source, destination, jmsMessageDomainDTO.getDomainCode()));
    }

    protected void validateMessagesMove(String source, JMSDestination destinationQueue, String[] jmsMessageIds) {
        if (jmsMessageIds.length == 0 || Arrays.stream(jmsMessageIds).anyMatch(StringUtils::isBlank)) {
            throw new RequestValidationException("No IDs provided for messages/action");
        }

        validateSourceAndDestination(source, destinationQueue);

        Arrays.stream(jmsMessageIds).forEach(msgId -> {
            InternalJmsMessage msg = internalJmsManager.getMessage(source, msgId);
            if (msg == null) {
                throw new RequestValidationException("Cannot find the message with id [" + msgId + "].");
            }
            String originalQueue = msg.getCustomProperties().get(JmsMessage.PROPERTY_ORIGINAL_QUEUE);
            if (StringUtils.isNotEmpty(originalQueue) && !matchQueue(destinationQueue, originalQueue)) {
                throw new RequestValidationException("Cannot move the message [" + msgId + "] to other than the original queue [" + originalQueue + "].");
            }
        });
    }

    private void validateSourceAndDestination(String source, JMSDestination destinationQueue) {
        if (destinationQueue == null) {
            throw new RequestValidationException("Destination cannot be null.");
        }
        if (source == null) {
            throw new RequestValidationException("Source cannot be null.");
        }
        if (StringUtils.equals(destinationQueue.getName(), source)) {
            throw new RequestValidationException("Source and destination queues names cannot be the same: [" + destinationQueue.getName() + "].");
        }
    }

    /**
     * @param destination
     * @return a JMSDestination with the name equal to the parameter
     * @throws RequestValidationException if no such destination queue is found
     */
    protected JMSDestination getValidJmsDestination(String destination) {
        Map<String, JMSDestination> destinations = getDestinations();
        JMSDestination jmsDestination = destinations.values().stream()
                .filter(dest -> StringUtils.equals(destination, dest.getName()))
                .findFirst()
                .orElseThrow(() -> new RequestValidationException("Cannot find destination with the name [" + destination + "]."));
        LOG.debug("Destination queue found: [{}]", jmsDestination.getName());
        return jmsDestination;
    }

    protected boolean matchQueue(JMSDestination queue, String queueName) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Trying to match original queue name [{}] with queue [{}] [{}] [{}]", queueName, queue, queue.getName(), queue.getFullyQualifiedName());
            if (queue.getProperties() != null) {
                queue.getProperties().keySet().forEach(p -> LOG.trace("Property [{}]: [{}]", p, queue.getProperty(p)));
            }
        }

        String shortQueueName = queueName.contains("!") ? queueName.substring(queueName.lastIndexOf('!') + 1) : queueName;
        if (StringUtils.equalsAny(queue.getName(), queueName, shortQueueName)) {
            LOG.trace("Queue name [{}] is matching original queue [{}]", queue.getName(), queueName);
            return true;
        }
        if (StringUtils.equals(queue.getFullyQualifiedName(), queueName)) {
            LOG.trace("Queue FQ name [{}] is matching original queue [{}]", queue.getFullyQualifiedName(), queueName);
            return true;
        }

        if (StringUtils.endsWithAny(queue.getName(),
                "." + queueName, "@" + queueName, "!" + queueName, "@" + shortQueueName, "!" + shortQueueName)) {
            LOG.trace("Queue name [{}] is matching original queue [{}]", queue.getName(), queueName);
            return true;
        }
        return false;
    }

    protected List<JMSMessageDomainDTO> getJMSMessageDomain(String source, String[] messageIds) {
        return Arrays.stream(messageIds).map(jmsMessageId -> new JMSMessageDomainDTO(jmsMessageId,
                retrieveDomainFromJMSMessage(source, jmsMessageId))).collect(Collectors.toList());
    }

    protected String retrieveDomainFromJMSMessage(String source, String jmsMessageId) {
        if (domibusConfigurationService.isSingleTenantAware()) {
            LOG.trace("JMS message [{}] doesn't have a domain property", jmsMessageId);
            return null;
        }
        //retrieve the domain
        JmsMessage jmsMessage = getMessage(source, jmsMessageId);
        return jmsMessage.getProperty(MessageConstants.DOMAIN);
    }

    @Override
    public JmsMessage consumeMessage(String source, String messageId) {
        messageId = RegExUtils.replaceAll(messageId, "'", "''");
        InternalJmsMessage internalJmsMessage = internalJmsManager.consumeMessage(source, messageId);
        return jmsMessageMapper.convert(internalJmsMessage);
    }

    @Override
    public long getDestinationSize(final String nameLike) {
        final Map<String, InternalJMSDestination> destinationsGroupedByFQName = internalJmsManager.findDestinationsGroupedByFQName();
        for (Map.Entry<String, InternalJMSDestination> entry : destinationsGroupedByFQName.entrySet()) {
            if (StringUtils.containsIgnoreCase(entry.getKey(), nameLike)) {
                final InternalJMSDestination value = entry.getValue();
                return value.getNumberOfMessages();
            }
        }
        return 0;
    }

    @Override
    public long getDestinationSize(JMSDestination jmsDestination) {
        return internalJmsManager.getDestinationCount(jmsDestinationMapper.convert(jmsDestination));
    }

    /**
     * tests if the given queue {@code jmsQueueInternalName} should be excluded from current queues of JMS Monitoring page
     * - when the user is logged as Admin domain and queue is defined as JMS Plugin queue
     */
    protected boolean jmsQueueInOtherDomain(final String jmsQueueInternalName) {
        /* multi-tenancy but not super-admin*/
        if (domibusConfigurationService.isMultiTenantAware() && !authUtils.isSuperAdmin()) {
            List<Domain> domainsList = domainService.getDomains();
            Domain currentDomain = domainContextProvider.getCurrentDomainSafely();

            List<Domain> domainsToCheck = domainsList.stream().filter(domain -> !domain.equals(DomainService.DEFAULT_DOMAIN) &&
                    !domain.equals(currentDomain)).collect(Collectors.toList());

            List<String> queuesToCheck = new ArrayList<>();
            for (Domain domain : domainsToCheck) {
                for (String jmsQueue : JMS_QUEUE_NAMES) {
                    queuesToCheck.add(domain.getCode() + "." + jmsQueue);
                }
            }

            return StringUtils.indexOfAny(jmsQueueInternalName, queuesToCheck.stream().toArray(String[]::new)) >= 0;
        }
        return false;
    }

    @Override
    public Collection<String> listPendingMessages(String queueName) {
        LOG.debug("Listing pending messages for queue [{}]", queueName);

        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }

        String originalUser = authUtils.getOriginalUserWithUnsecureLoginAllowed();
        LOG.info("Authorized as [{}]", originalUser == null ? "super user" : originalUser);


        /* if originalUser is null, all messages are returned */
        return getQueueElements(queueName, NotificationType.MESSAGE_RECEIVED, originalUser);
    }

    protected Collection<String> getQueueElements(String queueName, final NotificationType notificationType, final String finalRecipient) {
        return browseQueue(queueName, notificationType, finalRecipient);
    }

    protected Collection<String> browseQueue(String queueName, final NotificationType notificationType, final String finalRecipient) {
        final Collection<String> result = new ArrayList<>();
        final int intMaxPendingMessagesRetrieveCount = domibusPropertyProvider.getIntegerProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_LIST_PENDING_MESSAGES_MAX_COUNT);
        LOG.debug("Using maxPendingMessagesRetrieveCount [{}] limit", intMaxPendingMessagesRetrieveCount);

        String selector = MessageConstants.NOTIFICATION_TYPE + "='" + notificationType.name() + "'";

        if (finalRecipient != null) {
            selector += " and " + MessageConstants.FINAL_RECIPIENT + "='" + finalRecipient + "'";
        }
        selector = getDomainSelector(selector);

        List<JmsMessage> messages = browseClusterMessages(queueName, selector);
        LOG.info("[{}] messages selected from queue [{}] with selector [{}]", (messages != null ? messages.size() : 0), queueName, selector);

        int countOfMessagesIncluded = 0;
        for (JmsMessage message : messages) {
            String messageId = message.getCustomStringProperty(MessageConstants.MESSAGE_ID);
            result.add(messageId);
            countOfMessagesIncluded++;
            LOG.debug("Added MessageId [{}]", messageId);
            if ((intMaxPendingMessagesRetrieveCount != 0) && (countOfMessagesIncluded >= intMaxPendingMessagesRetrieveCount)) {
                LOG.info("Limit of pending messages to return has been reached [{}]", countOfMessagesIncluded);
                break;
            }
        }

        return result;
    }

    @Override
    @MDCKey({DomibusLogger.MDC_MESSAGE_ID, DomibusLogger.MDC_MESSAGE_ROLE, DomibusLogger.MDC_MESSAGE_ENTITY_ID})
    public void removeFromPending(String queueName, String messageId) throws MessageNotFoundException {
        if (!authUtils.isUnsecureLoginAllowed()) {
            authUtils.hasUserOrAdminRole();
        }

        //add messageId to MDC map
        if (StringUtils.isNotBlank(messageId)) {
            LOG.putMDC(DomibusLogger.MDC_MESSAGE_ID, messageId);
        }

        JmsMessage message = consumeMessage(queueName, messageId);
        if (message == null) {
            LOG.businessError(DomibusMessageCode.BUS_MSG_NOT_FOUND, messageId);
            throw new MessageNotFoundException(messageId, " pending for download");
        }
        LOG.businessInfo(DomibusMessageCode.BUS_MSG_CONSUMED, messageId, queueName);

    }

    private String getCompleteSelector(String selector, String originalQueue) {
        String domainSelector = getDomainSelector(selector);
        if (StringUtils.isNotBlank(originalQueue)) {
            if (StringUtils.isBlank(domainSelector)) {
                domainSelector = "originalQueue='" + originalQueue + "'";
            } else {
                domainSelector += " AND originalQueue='" + originalQueue + "'";
            }
        }
        return domainSelector;
    }

    protected String getDomainSelector(String selector) {
        if (!domibusConfigurationService.isMultiTenantAware()) {
            return selector;
        }
        if (authUtils.isSuperAdmin()) {
            return selector;
        }
        final Domain currentDomain = domainContextProvider.getCurrentDomain();
        String domainClause = "DOMAIN ='" + currentDomain.getCode() + "'";

        String result;
        if (StringUtils.isBlank(selector)) {
            result = domainClause;
        } else {
            result = selector + " AND " + domainClause;
        }
        return result;
    }
}
