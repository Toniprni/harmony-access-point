package eu.domibus.ext.delegate.services.cxf;

import eu.domibus.api.cxf.ProxyCxfUtilService;
import eu.domibus.ext.services.ProxyCxfUtilExtService;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.stereotype.Service;

/**
 * @author François Gautier
 * @since 5.0
 *
 * Delegate class allowing Domibus extension/plugin to use cxf Proxy configuration of Domibus
 */
@Service
public class ProxyCxfUtilDelegate implements ProxyCxfUtilExtService {
    private final ProxyCxfUtilService proxyCxfUtilService;

    public ProxyCxfUtilDelegate(ProxyCxfUtilService proxyCxfUtilService) {
        this.proxyCxfUtilService = proxyCxfUtilService;
    }

    public void configureProxy(HTTPClientPolicy httpClientPolicy, HTTPConduit httpConduit) {
        proxyCxfUtilService.configureProxy(httpClientPolicy, httpConduit);
    }

}
