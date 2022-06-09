package eu.domibus.core.plugin.notification;

import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.notification.AsyncNotificationConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Service
public class AsyncNotificationConfigurationService {

    public static final IDomibusLogger LOG = DomibusLoggerFactory.getLogger(AsyncNotificationConfigurationService.class);

    @Autowired(required = false)
    @Lazy //Lazy loading of AsyncNotificationConfiguration to avoid the circular dependency triggered by loading the BackendConnectors(SPIs) at startup
    protected List<AsyncNotificationConfiguration> asyncNotificationConfigurations;

    public AsyncNotificationConfiguration getAsyncPluginConfiguration(String backendName) {
        for (final AsyncNotificationConfiguration asyncNotificationConfiguration : asyncNotificationConfigurations) {
            if (matches(asyncNotificationConfiguration, backendName)) {
                return asyncNotificationConfiguration;
            }
        }
        LOG.trace("Not async configuration found for connector [{}]", backendName);
        return null;
    }

    protected boolean matches(AsyncNotificationConfiguration asyncNotificationConfiguration, String backendName) {
        if (asyncNotificationConfiguration.getBackendConnector() == null) {
            LOG.debug("Could not match connector for backend name [{}]: no configured connector", backendName);
            return false;
        }
        if (StringUtils.equalsIgnoreCase(asyncNotificationConfiguration.getBackendConnector().getName(), backendName)) {
            return true;
        }
        return false;
    }
}
