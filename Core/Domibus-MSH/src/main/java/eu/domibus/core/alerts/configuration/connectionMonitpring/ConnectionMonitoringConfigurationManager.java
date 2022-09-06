package eu.domibus.core.alerts.configuration.connectionMonitpring;

import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.alerts.configuration.AlertConfigurationManager;
import eu.domibus.core.alerts.configuration.ReaderMethodAlertConfigurationManager;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.service.AlertConfigurationService;
import eu.domibus.core.alerts.service.ConfigurationReader;
import eu.domibus.core.earchive.alerts.ArchivingNotificationFailedModuleConfiguration;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * Manages the reading of e-archiving notification failures alert alert configuration
 *
 * @author Ion Perpegel
 * @since 5.1
 */
@Service
public class ConnectionMonitoringConfigurationManager
        extends ReaderMethodAlertConfigurationManager<ConnectionMonitoringModuleConfiguration>
        implements AlertConfigurationManager {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(ConnectionMonitoringConfigurationManager.class);

    protected final DomibusPropertyProvider domibusPropertyProvider;

    protected final DomainContextProvider domainContextProvider;

    protected final AlertConfigurationService alertConfigurationService;

    public ConnectionMonitoringConfigurationManager(DomibusPropertyProvider domibusPropertyProvider,
                                                    DomainContextProvider domainContextProvider,
                                                    AlertConfigurationService alertConfigurationService) {
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.domainContextProvider = domainContextProvider;
        this.alertConfigurationService = alertConfigurationService;
    }

    @Override
    public AlertType getAlertType() {
        return AlertType.CONNECTION_MONITORING_FAILED;
    }

    @Override
    protected ConfigurationReader<ConnectionMonitoringModuleConfiguration> getReaderMethod() {
        return this::readConfiguration;
    }

    protected ConnectionMonitoringModuleConfiguration readConfiguration() {
        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
        try {
            final Boolean alertsActive = alertConfigurationService.isAlertModuleEnabled();
            final Boolean connMonitorAlertsActive = domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_CONNECTION_MONITORING_FAILED_ACTIVE);
            if (BooleanUtils.isNotTrue(alertsActive) || BooleanUtils.isNotTrue(connMonitorAlertsActive)) {
                return new ConnectionMonitoringModuleConfiguration();
            }

            final AlertLevel alertLevel = AlertLevel.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_ALERT_CONNECTION_MONITORING_FAILED_LEVEL));
            final String mailSubject = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_CONNECTION_MONITORING_FAILED_MAIL_SUBJECT);

            return new ConnectionMonitoringModuleConfiguration(alertLevel, mailSubject);
        } catch (Exception ex) {
            LOG.warn("Error while configuring alerts related to connection monitoring notifications for domain:[{}].", currentDomain, ex);
            return new ConnectionMonitoringModuleConfiguration();
        }
    }
}
