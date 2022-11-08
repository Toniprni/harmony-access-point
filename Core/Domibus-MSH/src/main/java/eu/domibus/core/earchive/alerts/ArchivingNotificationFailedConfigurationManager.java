//package eu.domibus.core.earchive.alerts;
//
//import eu.domibus.api.alerts.AlertLevel;
//import eu.domibus.api.multitenancy.Domain;
//import eu.domibus.api.multitenancy.DomainContextProvider;
//import eu.domibus.api.property.DomibusPropertyProvider;
//import eu.domibus.core.alerts.configuration.AlertConfigurationManager;
//import eu.domibus.core.alerts.configuration.ReaderMethodAlertConfigurationManager;
//import eu.domibus.core.alerts.model.common.AlertType;
//import eu.domibus.core.alerts.configuration.common.ConfigurationReader;
//import eu.domibus.logging.DomibusLoggerFactory;
//import org.apache.commons.lang3.BooleanUtils;
//import org.slf4j.Logger;
//import org.springframework.stereotype.Service;
//
//import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
//
///**
// * Manages the reading of e-archiving notification failures alert alert configuration
// *
// * @author Ion Perpegel
// * @since 5.0
// */
//@Service
//public class ArchivingNotificationFailedConfigurationManager
//        extends ReaderMethodAlertConfigurationManager<ArchivingNotificationFailedModuleConfiguration>
//        implements AlertConfigurationManager {
//
//    private static final Logger LOG = DomibusLoggerFactory.getLogger(ArchivingNotificationFailedConfigurationManager.class);
//
//    protected DomibusPropertyProvider domibusPropertyProvider;
//
//    protected DomainContextProvider domainContextProvider;
//
//    public ArchivingNotificationFailedConfigurationManager(DomibusPropertyProvider domibusPropertyProvider,
//                                                           DomainContextProvider domainContextProvider) {
//        this.domibusPropertyProvider = domibusPropertyProvider;
//        this.domainContextProvider = domainContextProvider;
//    }
//
//    @Override
//    public AlertType getAlertType() {
//        return AlertType.ARCHIVING_NOTIFICATION_FAILED;
//    }
//
//    @Override
//    protected ConfigurationReader<ArchivingNotificationFailedModuleConfiguration> getReaderMethod() {
//        return this::readConfiguration;
//    }
//
//    protected ArchivingNotificationFailedModuleConfiguration readConfiguration() {
//        Domain currentDomain = domainContextProvider.getCurrentDomainSafely();
//        try {
//            final Boolean alertsActive = domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_ACTIVE);
//            final Boolean earchiveAlertsActive = domibusPropertyProvider.getBooleanProperty(DOMIBUS_ALERT_EARCHIVING_NOTIFICATION_FAILED_ACTIVE);
//            if (BooleanUtils.isNotTrue(alertsActive) || BooleanUtils.isNotTrue(earchiveAlertsActive)) {
//                return new ArchivingNotificationFailedModuleConfiguration();
//            }
//            final AlertLevel alertLevel = AlertLevel.valueOf(domibusPropertyProvider.getProperty(DOMIBUS_ALERT_EARCHIVING_NOTIFICATION_FAILED_LEVEL));
//            final String mailSubject = domibusPropertyProvider.getProperty(DOMIBUS_ALERT_EARCHIVING_NOTIFICATION_FAILED_MAIL_SUBJECT);
//
//            return new ArchivingNotificationFailedModuleConfiguration(alertLevel, mailSubject);
//        } catch (Exception ex) {
//            LOG.warn("Error while configuring alerts related to e-archiving notifications for domain:[{}].", currentDomain, ex);
//            return new ArchivingNotificationFailedModuleConfiguration();
//        }
//    }
//}
