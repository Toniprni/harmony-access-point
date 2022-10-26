package eu.domibus.core.pmode.provider.dynamicdiscovery;

import com.google.common.io.ByteStreams;
import network.oxalis.vefa.peppol.lookup.api.FetcherResponse;
import network.oxalis.vefa.peppol.lookup.api.LookupException;
import network.oxalis.vefa.peppol.lookup.fetcher.AbstractFetcher;
import network.oxalis.vefa.peppol.mode.Mode;
import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

/**
 * @author idragusa
 * @since 5/22/18.
 */
/*
* This class is inherited from the BasicApacheFetcher of the peppol-lookup, to allow
* the configuration of the proxy (if enabled)
*
* */
public class ApacheFetcherForTest extends AbstractFetcher {

    protected RequestConfig requestConfig;
    protected CredentialsProvider credentialsProvider;

    public ApacheFetcherForTest(HttpHost proxy, CredentialsProvider credentialsProvider) {
        super(Mode.of(Mode.TEST));
        RequestConfig.Builder builder = RequestConfig.custom()
                .setConnectionRequestTimeout(timeout)
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout);

        if(proxy != null) {
            builder.setProxy(proxy);
        }
        if(credentialsProvider != null) {
            this.credentialsProvider = credentialsProvider;
        }

        requestConfig = builder.build();
    }

    @Override
    public FetcherResponse fetch(List<URI> uris) throws LookupException, FileNotFoundException {
        try (CloseableHttpClient httpClient = createClient()) {
            HttpGet httpGet = new HttpGet(uris.get(0));

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                switch (response.getStatusLine().getStatusCode()) {
                    case 200:
                        return new FetcherResponse(
                                new ByteArrayInputStream(ByteStreams.toByteArray(response.getEntity().getContent())),
                                // new BufferedInputStream(response.getEntity().getContent()),
                                response.containsHeader("X-SMP-Namespace") ?
                                        response.getFirstHeader("X-SMP-Namespace").getValue() : null
                        );
                    case 404:
                        throw new FileNotFoundException(uris.get(0).toString());
                    default:
                        throw new LookupException(String.format(
                                "Received code %s for lookup. URI: %s", response.getStatusLine().getStatusCode(), uris.get(0)));
                }
            }
        } catch (SocketTimeoutException | SocketException | UnknownHostException e) {
            throw new LookupException(String.format("Unable to fetch '%s'", uris.get(0)), e);
        } catch (LookupException | FileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new LookupException(e.getMessage(), e);
        }
    }

    protected CloseableHttpClient createClient() {
        HttpClientBuilder builder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        if(credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return builder.build();
    }
}
