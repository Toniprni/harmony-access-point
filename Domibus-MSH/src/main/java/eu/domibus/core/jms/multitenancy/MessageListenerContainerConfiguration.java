package eu.domibus.core.jms.multitenancy;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.DomibusJMSConstants;
import eu.domibus.core.earchive.listener.EArchiveListener;
import eu.domibus.core.ebms3.sender.MessageSenderErrorHandler;
import eu.domibus.core.ebms3.sender.MessageSenderListener;
import eu.domibus.core.message.pull.PullMessageSender;
import eu.domibus.core.message.pull.PullReceiptListener;
import eu.domibus.core.message.retention.RetentionListener;
import eu.domibus.core.message.splitandjoin.LargeMessageSenderListener;
import eu.domibus.core.message.splitandjoin.SplitAndJoinListener;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JndiDestinationResolver;
import org.springframework.scheduling.SchedulingTaskExecutor;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import java.util.Optional;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.jms.spi.InternalJMSConstants.*;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@Configuration
public class MessageListenerContainerConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MessageListenerContainerConfiguration.class);
    public static final String PROPERTY_LARGE_FILES_CONCURRENCY = DOMIBUS_DISPATCHER_LARGE_FILES_CONCURRENCY;
    public static final String PROPERTY_SPLIT_AND_JOIN_CONCURRENCY = DOMIBUS_DISPATCHER_SPLIT_AND_JOIN_CONCURRENCY;
    private static final String PROPERTY_RETENTION_JMS_CONCURRENCY = DOMIBUS_RETENTION_JMS_CONCURRENCY;

    public static final String DISPATCH_CONTAINER = "dispatchContainer";
    public static final String SEND_LARGE_MESSAGE_CONTAINER = "sendLargeMessageContainer";
    public static final String SPLIT_AND_JOIN_CONTAINER = "splitAndJoinContainer";
    public static final String PULL_RECEIPT_CONTAINER = "pullReceiptContainer";
    public static final String RETENTION_CONTAINER = "retentionContainer";
    public static final String PULL_MESSAGE_CONTAINER = "pullMessageContainer";
    public static final String EARCHIVE_MESSAGE_CONTAINER = "eArchiveMessageContainer";

    @Autowired
    @Qualifier(SEND_MESSAGE_QUEUE)
    private Queue sendMessageQueue;

    @Autowired
    @Qualifier(SEND_PULL_RECEIPT_QUEUE)
    private Queue sendPullReceiptQueue;

    @Autowired
    @Qualifier(EARCHIVE_QUEUE)
    private Queue eArchiveQueue;

    @Autowired
    @Qualifier(PULL_MESSAGE_QUEUE)
    private Queue pullMessageQueue;

    @Autowired
    @Qualifier(SEND_LARGE_MESSAGE_QUEUE)
    private Queue sendLargeMessageQueue;

    @Autowired
    @Qualifier(SPLIT_AND_JOIN_QUEUE)
    private Queue splitAndJoinQueue;

    @Autowired
    @Qualifier(RETENTION_MESSAGE_QUEUE)
    private Queue retentionMessageQueue;

    @Autowired
    private MessageSenderListener messageSenderListener;

    @Autowired
    private LargeMessageSenderListener largeMessageSenderListener;

    @Autowired
    private SplitAndJoinListener splitAndJoinListener;

    @Autowired
    private PullReceiptListener pullReceiptListener;

    @Autowired
    private EArchiveListener eArchiveListener;

    @Autowired
    private RetentionListener retentionListener;

    @Autowired
    PullMessageSender pullMessageListener;

    @Autowired
    @Qualifier(DomibusJMSConstants.DOMIBUS_JMS_CONNECTION_FACTORY)
    private ConnectionFactory connectionFactory;

    @Autowired
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    Optional<JndiDestinationResolver> internalDestinationResolver;

    @Qualifier("taskExecutor")
    @Autowired
    protected SchedulingTaskExecutor schedulingTaskExecutor;

    @Autowired
    @Qualifier("messageSenderErrorHandler")
    protected MessageSenderErrorHandler messageSenderErrorHandler;

    @Bean(name = DISPATCH_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendMessageListener(Domain domain, String selector, String concurrencyPropertyName) {
        LOG.debug("Instantiating the DefaultMessageListenerContainer for domain [{}] with selector [{}] and concurrency property [{}]", domain, selector, concurrencyPropertyName);

        DefaultMessageListenerContainer defaultMessageListenerContainer = createDefaultMessageListenerContainer(domain, connectionFactory, sendMessageQueue,
                messageSenderListener, concurrencyPropertyName, selector
        );
        defaultMessageListenerContainer.setErrorHandler(messageSenderErrorHandler);
        return defaultMessageListenerContainer;
    }

    /**
     * Creates the large message JMS listener(domain dependent)
     */
    @Bean(name = SEND_LARGE_MESSAGE_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSendLargeMessageListener(Domain domain) {
        LOG.debug("Instantiating the createSendLargeMessageListenerContainer for domain [{}]", domain);

        DefaultMessageListenerContainer defaultMessageListenerContainer = createDefaultMessageListenerContainer(domain, connectionFactory, sendLargeMessageQueue,
                largeMessageSenderListener, PROPERTY_LARGE_FILES_CONCURRENCY
        );
        defaultMessageListenerContainer.setErrorHandler(messageSenderErrorHandler);
        return defaultMessageListenerContainer;
    }

    /**
     * Creates the SplitAndJoin JMS listener(domain dependent)
     */
    @Bean(name = SPLIT_AND_JOIN_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createSplitAndJoinListener(Domain domain) {
        LOG.debug("Instantiating the createSplitAndJoinListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, splitAndJoinQueue,
                splitAndJoinListener, PROPERTY_SPLIT_AND_JOIN_CONCURRENCY
        );
    }

    @Bean(name = PULL_RECEIPT_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createPullReceiptListener(Domain domain) {
        LOG.debug("Instantiating the createPullReceiptListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, sendPullReceiptQueue,
                pullReceiptListener, DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY
        );
    }

    @Bean(name = EARCHIVE_MESSAGE_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createEArchiveListener(Domain domain) {
        LOG.debug("Instantiating the createPullReceiptListener for domain [{}]", domain);
        return createDefaultMessageListenerContainer(domain, connectionFactory, eArchiveQueue,
                eArchiveListener, DOMIBUS_EARCHIVE_QUEUE_CONCURRENCY
        );
    }

    /**
     * Creates the Retention Message JMS listener (domain dependent)
     *
     * @param domain the domain to which this bean is created for
     * @return the retention listener prototype bean dedicated to the provided domain
     */
    @Bean(name = RETENTION_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createRetentionListener(Domain domain) {
        LOG.debug("Instantiating the createRetentionListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, retentionMessageQueue,
                retentionListener, PROPERTY_RETENTION_JMS_CONCURRENCY);
    }

    @Bean(name = PULL_MESSAGE_CONTAINER)
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public DefaultMessageListenerContainer createPullMessageListener(Domain domain) {
        LOG.debug("Instantiating the pullMessageListener for domain [{}]", domain);

        return createDefaultMessageListenerContainer(domain, connectionFactory, pullMessageQueue,
                pullMessageListener::processPullRequest, DOMIBUS_PULL_RECEIPT_QUEUE_CONCURRENCY, true, null);
    }

    /**
     * It will create a {@code DefaultMessageListenerContainer}
     *
     * @param domain                    domain
     * @param connectionFactory         JMS connection factory
     * @param destination               JMS queue
     * @param messageListener           JMS message listener
     * @param domainPropertyConcurrency domain property key for retrieving queue concurrency value
     */
    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener,
                                                                                  String domainPropertyConcurrency, String selector) {
        return createDefaultMessageListenerContainer(domain, connectionFactory, destination, messageListener, domainPropertyConcurrency, false, selector);
    }

    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener,
                                                                                  String domainPropertyConcurrency) {
        return createDefaultMessageListenerContainer(domain, connectionFactory, destination, messageListener, domainPropertyConcurrency, false, null);
    }

    private DefaultMessageListenerContainer createDefaultMessageListenerContainer(Domain domain, ConnectionFactory connectionFactory, Queue destination,
                                                                                  MessageListener messageListener,
                                                                                  String domainPropertyConcurrency, boolean useInternalDestinationResolver, String selector) {
        DefaultMessageListenerContainer messageListenerContainer = new DomainMessageListenerContainer(domain);

        String messageSelector = MessageConstants.DOMAIN + "='" + domain.getCode() + "'";
        if (StringUtils.isNotBlank(selector)) {
            messageSelector += " AND " + selector;
        }
        LOG.info("Using message selector [{}]", messageSelector);

        messageListenerContainer.setMessageSelector(messageSelector);

        messageListenerContainer.setConnectionFactory(connectionFactory);
        messageListenerContainer.setDestination(destination);
        messageListenerContainer.setMessageListener(messageListener);

        final String concurrency = domibusPropertyProvider.getProperty(domain, domainPropertyConcurrency);
        if (StringUtils.isEmpty(concurrency)) {
            throw new DomibusCoreException("Concurrency value not defined for property [" + domainPropertyConcurrency + "]");
        }
        messageListenerContainer.setConcurrency(concurrency);

        manageTimeout(domain, destination, domainPropertyConcurrency, messageListenerContainer);

        messageListenerContainer.setSessionTransacted(true);
        messageListenerContainer.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        messageListenerContainer.setTaskExecutor(schedulingTaskExecutor);

        messageListenerContainer.afterPropertiesSet();

        if (useInternalDestinationResolver && internalDestinationResolver.isPresent()) {
            messageListenerContainer.setDestinationResolver(internalDestinationResolver.get());
        }

        LOG.debug("DefaultMessageListenerContainer initialized for domain [{}] with concurrency=[{}]", domain, concurrency);
        return messageListenerContainer;
    }

    protected void manageTimeout(Domain domain, Queue destination, String domainPropertyConcurrency, DefaultMessageListenerContainer messageListenerContainer) {
        String timeoutPropertyName = getTimeoutPropertyName(domainPropertyConcurrency);
        boolean isTimeoutDefined = domibusPropertyProvider.containsPropertyKey(timeoutPropertyName);
        if (!isTimeoutDefined) {
            LOG.debug("The timeout property [{}] for the queue [{}] is not defined.", timeoutPropertyName, destination);
            return;
        }
        String timeoutPropertyValue = domibusPropertyProvider.getProperty(domain, timeoutPropertyName);
        final Integer timeout = NumberUtils.toInt(timeoutPropertyValue);
        if (timeout <= 0) {
            LOG.debug("The timeout property value for the queue [{}] is badly or not defined: [{}].", destination, timeoutPropertyValue);
            return;
        }
        messageListenerContainer.setTransactionTimeout(timeout);
        LOG.info("The timeout [{}] was set for the queue [{}].", timeout, destination);
    }

    private String getTimeoutPropertyName(String concurrencyPropertyName) {
        return concurrencyPropertyName.substring(0, concurrencyPropertyName.lastIndexOf(".")) + ".timeout";
    }

}
