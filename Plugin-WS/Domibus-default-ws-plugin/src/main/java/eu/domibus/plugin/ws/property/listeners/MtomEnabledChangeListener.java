package eu.domibus.plugin.ws.property.listeners;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.property.PluginPropertyChangeListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.xml.ws.Endpoint;
import javax.xml.ws.soap.SOAPBinding;

import static eu.domibus.plugin.ws.property.WSPluginPropertyManager.MTOM_ENABLED_PROPERTY;

/**
 * Handles the change of wsplugin.mtom.enabled property of backendInterfaceEndpoint
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class MtomEnabledChangeListener implements PluginPropertyChangeListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(MtomEnabledChangeListener.class);

    private final Endpoint backendInterfaceEndpoint;
    private final Endpoint backendInterfaceEndpointDeprecated;

    public MtomEnabledChangeListener(@Qualifier("backendInterfaceEndpoint") Endpoint backendInterfaceEndpoint, @Qualifier("backendInterfaceEndpointDeprecated") Endpoint backendInterfaceEndpointDeprecated) {
        this.backendInterfaceEndpoint = backendInterfaceEndpoint;
        this.backendInterfaceEndpointDeprecated = backendInterfaceEndpointDeprecated;
    }

    @Override
    public boolean handlesProperty(String propertyName) {
        boolean doesHandle = StringUtils.equals(propertyName, MTOM_ENABLED_PROPERTY);
        LOG.trace("Handling [{}] property: [{}]", propertyName, doesHandle);
        return doesHandle;
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        LOG.trace("Setting [{}] property to [{}] on domain: [{}]", propertyName, propertyValue, domainCode);
        backendInterfaceEndpoint.getProperties().put("mtom-enabled", propertyValue);
        backendInterfaceEndpointDeprecated.getProperties().put("mtom-enabled", propertyValue);
    }
}
