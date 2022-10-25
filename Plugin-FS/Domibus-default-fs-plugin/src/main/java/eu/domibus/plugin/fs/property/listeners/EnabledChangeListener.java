package eu.domibus.plugin.fs.property.listeners;

import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.fs.FSPluginImpl;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Component;

import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.DOMAIN_ENABLED;

/**
 * @author Ion Perpegel
 * @since 5.0
 * <p>
 * Handles enabling/disabling of fs-plugin for the current domain.
 */
@Component
public class EnabledChangeListener implements PluginPropertyChangeListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EnabledChangeListener.class);

    public static final String[] FSPLUGIN_JOB_NAMES = TriggerChangeListener.CRON_PROPERTY_NAMES_TO_JOB_MAP.values().toArray(new String[]{}); //NOSONAR

    final protected FSPluginImpl backendFSPlugin;

    public EnabledChangeListener(FSPluginImpl backendFSPlugin) {
        this.backendFSPlugin = backendFSPlugin;
    }

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equals(DOMAIN_ENABLED, propertyName);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) throws DomibusPropertyExtException {
        LOG.debug("Executing enabled listener on domain [{}] for property [{}] with value [{}]", domainCode, propertyName, propertyValue);
        boolean enable = BooleanUtils.toBoolean(propertyValue);
        backendFSPlugin.doSetEnabled(domainCode, enable);
    }

}
