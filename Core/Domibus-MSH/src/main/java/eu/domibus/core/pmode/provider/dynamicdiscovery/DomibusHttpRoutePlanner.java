package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.core.ssl.offload.SslOffloadService;
import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.springframework.stereotype.Service;

import java.net.URL;

/**
 * A Domibus route planner that allows switching the SSL connections to HTTP for SSL offloading scenarios.
 *
 * @author Sebastian-Ion TINCU
 * @since 5.0
 */
@Service
public class DomibusHttpRoutePlanner extends DefaultRoutePlanner {

    private static final IDomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusHttpRoutePlanner.class);

    private final SslOffloadService sslOffloadService;

    public DomibusHttpRoutePlanner(SslOffloadService sslOffloadService) {
        super(null);
        this.sslOffloadService = sslOffloadService;
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
        HttpRoute route = super.determineRoute(host, request, context);

        URL url = getUrl(host);

        if(sslOffloadService.isSslOffloadEnabled(url)) {
            LOG.info("Switch [{}] to an HTTP connection for SSL offloading", host);
            route = route.getProxyHost() == null
                    ? new HttpRoute(route.getTargetHost(), route.getLocalAddress(), false)
                    : new HttpRoute(route.getTargetHost(), route.getLocalAddress(), route.getProxyHost(), false);
        }
        return route;
    }

    private URL getUrl(HttpHost host) {
        URL url = null;
        try {
            LOG.debug("Convert the host String representation [{}] to a URL", host);
            url = new URL(host.toURI());
        } catch (Exception e) {
            LOG.error("An error occurred when creating a URL from the host String representation", e);
        }
        return url;
    }
}
