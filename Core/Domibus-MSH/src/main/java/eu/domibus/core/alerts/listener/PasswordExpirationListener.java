package eu.domibus.core.alerts.listener;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.alerts.dao.EventDao;
import eu.domibus.core.alerts.model.common.EventType;
import eu.domibus.core.alerts.model.service.Alert;
import eu.domibus.core.alerts.model.service.Event;
import eu.domibus.core.alerts.service.AlertService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_JMS_QUEUE_ALERT;
import static eu.domibus.core.alerts.service.EventServiceImpl.ALERT_JMS_LISTENER_CONTAINER_FACTORY;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@Component
public class PasswordExpirationListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PasswordExpirationListener.class);

    @Autowired
    private AlertService alertService;

    @Autowired
    private DomainContextProvider domainContextProvider;

    @Autowired
    private EventDao eventDao;

    @Autowired
    private DatabaseUtil databaseUtil;

    @JmsListener(containerFactory = ALERT_JMS_LISTENER_CONTAINER_FACTORY, destination = "${"+ DOMIBUS_JMS_QUEUE_ALERT + "}",
            selector = "selector = '" + EventType.QuerySelectors.PASSWORD_EXPIRATION + "'")
    // Intentionally used just one selector value for all 4 types of events
    public void onPasswordEvent(final Event event, @Header(name = "DOMAIN", required = false) String domain) {
        triggerAlert(event, domain);
    }

    private void triggerAlert(Event event, String domain) {
        if (domain == null) {
            domainContextProvider.clearCurrentDomain();
        } else {
            domainContextProvider.setCurrentDomain(domain);
        }
        LOG.putMDC(DomibusLogger.MDC_USER, databaseUtil.getDatabaseUserName());

        //find the corresponding persisted event
        eu.domibus.core.alerts.model.persist.Event entity = eventDao.read(event.getEntityId());
        if (entity != null) {
            final Alert alertOnEvent = alertService.createAlertOnEvent(event);
            alertService.enqueueAlert(alertOnEvent);
        }
    }

}
