package eu.domibus.core.earchive.alerts;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.service.AlertService;
import eu.domibus.core.alerts.service.EventService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_JMS_QUEUE_ALERT;
import static eu.domibus.core.alerts.service.EventServiceImpl.ALERT_JMS_LISTENER_CONTAINER_FACTORY;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Component
public class ArchivingEventListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ArchivingEventListener.class);

    private final EventService eventService;

    private final AlertService alertService;

    private final DomainContextProvider domainContextProvider;

    public ArchivingEventListener(EventService eventService,
                                  AlertService alertService,
                                  DomainContextProvider domainContextProvider) {
        this.eventService = eventService;
        this.alertService = alertService;
        this.domainContextProvider = domainContextProvider;
    }

    @JmsListener(containerFactory = ALERT_JMS_LISTENER_CONTAINER_FACTORY, destination = "${"+ DOMIBUS_JMS_QUEUE_ALERT + "}",
            selector = "selector = 'ARCHIVING_MESSAGES_NON_FINAL' " +
                    "or selector = 'ARCHIVING_NOTIFICATION_FAILED' " +
                    "or selector = 'ARCHIVING_START_DATE_STOPPED'")
    @Transactional
    public void onEvent(final Event event, final @Header(name = "DOMAIN", required = false) String domain) {
        saveEventAndTriggerAlert(event, domain);
    }

    private void saveEventAndTriggerAlert(Event event, final String domain) {
        domainContextProvider.setCurrentDomain(domain);

        LOG.trace("Persisting e-archiving notification alert");
        eventService.persistEvent(event);
        final Alert alertOnEvent = alertService.createAlertOnEvent(event);
        alertService.enqueueAlert(alertOnEvent);
    }

}
