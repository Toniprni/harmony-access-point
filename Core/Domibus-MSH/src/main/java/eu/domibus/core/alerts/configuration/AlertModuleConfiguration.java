package eu.domibus.core.alerts.configuration;

import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.core.alerts.model.common.AlertType;
import eu.domibus.core.alerts.model.service.Event;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public interface AlertModuleConfiguration {

    AlertType getAlertType();

    String getMailSubject();

    boolean isActive();

    AlertLevel getAlertLevel(final Event event);

}
