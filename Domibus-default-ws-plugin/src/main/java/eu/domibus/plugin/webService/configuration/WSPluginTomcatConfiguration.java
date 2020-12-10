package eu.domibus.plugin.webService.configuration;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.environment.TomcatCondition;
import eu.domibus.plugin.webService.property.WSPluginPropertyManager;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Class responsible for the configuration of the plugin for Tomcat
 *
 * @author Cosmin Baciu
 * @since 4.2
 */
@Conditional(TomcatCondition.class)
@Configuration
public class WSPluginTomcatConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(WSPluginTomcatConfiguration.class);

    @Bean("notifyBackendWebServiceQueue")
    public ActiveMQQueue notifyBackendFSQueue() {
        return new ActiveMQQueue("domibus.notification.webservice");
    }

    @Bean("wsPluginSendQueue")
    @Conditional(TomcatCondition.class)
    public ActiveMQQueue fsPluginSendQueue(WSPluginPropertyManager wsPluginPropertyManager) {
        String queueName = wsPluginPropertyManager.getKnownPropertyValue(WSPluginPropertyManager.DISPATCHER_SEND_QUEUE_NAME);
        LOG.debug("Using ws plugin send queue name [{}]", queueName);
        return new ActiveMQQueue(queueName);
    }

}
