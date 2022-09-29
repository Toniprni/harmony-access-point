package eu.domibus.common;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This event is used to transfer information on the failure to received message event, from the core to the plugins implementing PUSH methods.
 *
 * @author Cosmin Baciu
 * @since 3.2.2
 */
public class MessageReceiveFailureEvent implements MessageEvent, Serializable {

    private static final long serialVersionUID = -4629417035350242509L;
    protected String messageId;
    protected String endpoint;
    protected String service;
    protected String serviceType;
    protected String action;
    protected ErrorResult errorResult;
    protected Map<String, String> properties = new HashMap<>(); //NOSONAR
    protected Long messageEntityId;

    @Override
    public Long getMessageEntityId() {
        return messageEntityId;
    }

    public void setMessageEntityId(Long messageEntityId) {
        this.messageEntityId = messageEntityId;
    }

    @Override
    public Map<String, String> getProps() {
        return properties;
    }

    @Override
    public void addProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public ErrorResult getErrorResult() {
        return errorResult;
    }

    public void setErrorResult(ErrorResult errorResult) {
        this.errorResult = errorResult;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
