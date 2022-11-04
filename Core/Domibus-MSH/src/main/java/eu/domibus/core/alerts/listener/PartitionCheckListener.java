package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
//import eu.domibus.core.alerts.configuration.partitions.PartitionsConfigurationManager;
//import eu.domibus.core.alerts.configuration.partitions.PartitionsModuleConfiguration;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.common.PartitionCheckEvent;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.service.AlertService;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.core.earchive.alerts.FrequencyAlertConfiguration;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_JMS_QUEUE_ALERT;
import static eu.domibus.core.alerts.service.EventServiceImpl.ALERT_JMS_LISTENER_CONTAINER_FACTORY;

/**
 * @author idragusa
 * @since 5.0
 */
@Component
public class PartitionCheckListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartitionCheckListener.class);
    private AlertService alertService;
    private DomainContextProvider domainContextProvider;
    private EventDao eventDao;
    private DatabaseUtil databaseUtil;
    private EventService eventService;
//    private PartitionsConfigurationManager partitionsConfigurationManager;

    public PartitionCheckListener(AlertService alertService,
                                  DomainContextProvider domainContextProvider,
                                  EventDao eventDao, DatabaseUtil databaseUtil,
                                  EventService eventService
//                                  PartitionsConfigurationManager partitionsConfigurationManager
                                  ) {
        this.alertService = alertService;
        this.domainContextProvider = domainContextProvider;
        this.eventDao = eventDao;
        this.eventService = eventService;
//        this.partitionsConfigurationManager = partitionsConfigurationManager;
    }

    @JmsListener(containerFactory = ALERT_JMS_LISTENER_CONTAINER_FACTORY, destination = "${"+ DOMIBUS_JMS_QUEUE_ALERT + "}",
            selector = "selector ='" + EventType.QuerySelectors.PARTITION_CHECK + "'")
    public void onDeleteExpiredPartitionEvent(final Event event, @Header(name = "DOMAIN", required = false) String domain) {
        saveEventAndTriggerAlert(event, domain);
    }

    private void saveEventAndTriggerAlert(Event event, @Header(name = "DOMAIN") String domain) {
        domainContextProvider.setCurrentDomain(domain);
//        PartitionsModuleConfiguration partitionsModuleConfiguration = partitionsConfigurationManager.getConfiguration();
        FrequencyAlertConfiguration partitionsModuleConfiguration = (FrequencyAlertConfiguration)AlertType.PARTITION_CHECK.getConfiguration();
        final String name = PartitionCheckEvent.PARTITION_NAME.name();
        eu.domibus.core.alerts.model.persist.Event persistedEvent = eventDao.findWithTypeAndPropertyValue(event.getType(), name, event.findStringProperty(name).orElse(null));

        if (!eventService.shouldCreateAlert(persistedEvent, partitionsModuleConfiguration.getFrequency())) {
            return;
        }
        event.setLastAlertDate(LocalDate.now());

        LOG.debug("Partition expiration event:[{}] for domain:[{}]", event, domain);
        eventService.persistEvent(event);

        final Alert alertOnEvent = alertService.createAlertOnEvent(event);
        alertService.enqueueAlert(alertOnEvent);
    }
}
