package eu.domibus.core.alerts.configuration.connectionMonitpring;

import eu.domibus.api.alerts.AlertLevel;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.core.alerts.configuration.AlertModuleConfigurationBase;
import eu.domibus.core.alerts.model.common.AlertType;

import java.util.List;

/**
 * @author Ion Perpegel
 * @since 5.1
 */
public class ConnectionMonitoringModuleConfiguration extends AlertModuleConfigurationBase {

    private int frequency;

    private List<String> enabledParties;

//    public ConnectionMonitoringModuleConfiguration(final int frequency, final AlertLevel alertLevel, final String mailSubject, final List<String> enabledParties) {
//        super(AlertType.CONNECTION_MONITORING_FAILED, alertLevel, mailSubject);
//        this.enabledParties = enabledParties;
//        this.frequency = frequency;
//    }

    public ConnectionMonitoringModuleConfiguration() {
        super(AlertType.CONNECTION_MONITORING_FAILED);
    }

    public int getFrequency() {
        return frequency;
    }

    public boolean shouldGenerateAlert(MessageStatus messageStatus, String toParty) {
        return isActive()
                && MessageStatus.getUnsuccessfulStates().contains(messageStatus)
                && enabledParties.contains(toParty);
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public void setEnabledParties(List<String> enabledParties) {
        this.enabledParties = enabledParties;
    }

    @Override
    public String toString() {
        return "ConnectionMonitoringConfiguration{" +
                "Active=" + isActive() +
                "Frequency=" + getFrequency() +
                "EnabledParties=" + enabledParties +
                '}';
    }

}
