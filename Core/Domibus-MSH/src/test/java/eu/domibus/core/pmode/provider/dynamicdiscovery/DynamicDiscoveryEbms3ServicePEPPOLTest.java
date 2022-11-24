package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.proxy.ProxyUtil;
import mockit.*;
import mockit.integration.junit4.JMockit;
import network.oxalis.vefa.peppol.common.lang.PeppolParsingException;
import network.oxalis.vefa.peppol.common.model.*;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.locator.BusdoxLocator;
import network.oxalis.vefa.peppol.mode.Mode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.ObjectProvider;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static eu.domibus.core.certificate.CertificateTestUtils.loadCertificateFromJKSFile;
import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ioana DRAGUSANU
 * @author Sebastian-Ion TINCU
 * @since 3.2.5
 */
@RunWith(JMockit.class)
public class DynamicDiscoveryEbms3ServicePEPPOLTest {

    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/";
    private static final String TEST_KEYSTORE = "testkeystore.jks";

    //The (sub)domain of the SML, e.g. acc.edelivery.tech.ec.europa.eu
    //private static final String TEST_SML_ZONE = "isaitb.acc.edelivery.tech.ec.europa.eu";
    private static final String TEST_SML_ZONE = "acc.edelivery.tech.ec.europa.eu";

    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String TEST_KEYSTORE_PASSWORD = "1234";

    private static final String TEST_RECEIVER_ID = "0088:unknownRecipient";
    private static final String TEST_RECEIVER_ID_TYPE = "iso6523-actorid-upis";
    private static final String TEST_ACTION_VALUE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:www.cenbii.eu:transaction:biitrns014:ver2.0:extended:urn:www.peppol.eu:bis:peppol5a:ver2.0::2.1";
    private static final String TEST_SERVICE_VALUE = "scheme::serviceValue";
    private static final String TEST_SERVICE_TYPE = "serviceType";
    private static final String TEST_INVALID_SERVICE_VALUE = "invalidServiceValue";
    private static final String DOMAIN = "default";

    private static final String ADDRESS = "http://localhost:9090/anonymous/msh";

    @Injectable
    private DomainContextProvider domainProvider;

    @Injectable
    private MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    private DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    private CertificateService certificateService;

    @Injectable
    private ProxyUtil proxyUtil;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    private DomibusHttpRoutePlanner domibusHttpRoutePlanner;

    @Injectable
    private ObjectProvider<DomibusCertificateValidator> domibusCertificateValidators;

    @Injectable
    private ObjectProvider<BusdoxLocator> busdoxLocators;

    @Injectable
    private ObjectProvider<DomibusApacheFetcher> domibusApacheFetchers;

    @Injectable
    private ObjectProvider<EndpointInfo> endpointInfos;

    @Injectable
    private DomibusCertificateValidator domibusCertificateValidator;

    @Injectable
    private BusdoxLocator busdoxLocator;

    @Injectable
    private DomibusApacheFetcher domibusApacheFetcher;

    @Injectable
    private EndpointInfo endpointInfo;

    @Injectable
    private DynamicDiscoveryUtil dynamicDiscoveryUtil;

    @Tested
    private DynamicDiscoveryServicePEPPOL dynamicDiscoveryServicePEPPOL;

    private String transportProfileAS4;

    @Before
    public void setup() {
        new NonStrictExpectations() {{
            domibusCertificateValidators.getObject(any, any, anyString, anyString);
            result = domibusCertificateValidator;

            busdoxLocators.getObject(anyString);
            result = busdoxLocator;

            domibusApacheFetchers.getObject(any, proxyUtil, domibusHttpRoutePlanner);
            result = domibusApacheFetcher;

            endpointInfos.getObject(anyString, any);
            result = endpointInfo;
        }};
    }

    @Test
    public void testLookupInformationMock(@Capturing LookupClient smpClient) throws Exception {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SMLZONE);
            result = TEST_SML_ZONE;

            domibusPropertyProvider.getProperty(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_MODE);
            result = Mode.TEST;

            transportProfileAS4 = TransportProfile.AS4.getIdentifier();
            ServiceMetadata sm = buildServiceMetadata();
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentTypeIdentifier) any);
            result = sm;

            domibusPropertyProvider.getProperty(DOMIBUS_DYNAMICDISCOVERY_TRANSPORTPROFILEAS_4);
            result = transportProfileAS4;

            endpointInfo.getAddress();
            result = ADDRESS;
        }};

        EndpointInfo endpoint = dynamicDiscoveryServicePEPPOL.lookupInformation(DOMAIN, TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
        assertNotNull(endpoint);
        assertEquals(ADDRESS, endpoint.getAddress());

        new Verifications() {{
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentTypeIdentifier) any);
        }};
    }

    @Test
    public void testLookupInformationMockOtherTransportProfile(final @Capturing LookupClient smpClient) throws Exception {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SMLZONE);
            result = TEST_SML_ZONE;

            domibusPropertyProvider.getProperty(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_MODE);
            result = Mode.TEST;

            transportProfileAS4 = "AS4_other_transport_profile";
            ServiceMetadata sm = buildServiceMetadata();
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentTypeIdentifier) any);
            result = sm;

            domibusPropertyProvider.getProperty(DOMIBUS_DYNAMICDISCOVERY_TRANSPORTPROFILEAS_4);
            result = transportProfileAS4;

            endpointInfo.getAddress();
            result = ADDRESS;
        }};

        EndpointInfo endpoint = dynamicDiscoveryServicePEPPOL.lookupInformation(DOMAIN, TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
        assertNotNull(endpoint);
        assertEquals(ADDRESS, endpoint.getAddress());

        new Verifications() {{
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentTypeIdentifier) any);
        }};
    }

    @Test
    public void getPartyIdTypeTestForNull() {
        final String URN_TYPE_VALUE = "urn:fdc:peppol.eu:2017:identifiers:ap";
        new Expectations() {{
            dynamicDiscoveryUtil.getTrimmedDomibusProperty(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_PARTYID_TYPE);
            result = URN_TYPE_VALUE;
            times = 1;
        }};
        String partyIdType = dynamicDiscoveryServicePEPPOL.getPartyIdType();
        Assert.assertEquals(partyIdType, URN_TYPE_VALUE);
    }

    @Test
    public void getPartyIdTypeTestForEmpty() {
        new Expectations() {{
            dynamicDiscoveryUtil.getTrimmedDomibusProperty(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_PARTYID_TYPE);
            result = null;
            times = 1;
        }};
        String partyIdType = dynamicDiscoveryServicePEPPOL.getPartyIdType();
        Assert.assertNull(partyIdType);
    }

    @Test(expected = ConfigurationException.class)
    public void testLookupInformationNotFound(final @Capturing LookupClient smpClient) throws Exception {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SMLZONE);
            result = TEST_SML_ZONE;

            domibusPropertyProvider.getProperty(DOMIBUS_DYNAMICDISCOVERY_PEPPOLCLIENT_MODE);
            result = Mode.TEST;

            transportProfileAS4 = TransportProfile.AS4.getIdentifier();
            ServiceMetadata sm = buildServiceMetadata();
            smpClient.getServiceMetadata((ParticipantIdentifier) any, (DocumentTypeIdentifier) any);
            result = sm;

        }};

        dynamicDiscoveryServicePEPPOL.lookupInformation(DOMAIN, TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_INVALID_SERVICE_VALUE, TEST_SERVICE_TYPE);
    }


    private ServiceMetadata buildServiceMetadata() {

        X509Certificate testData = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        ProcessIdentifier processIdentifier;
        try {
            processIdentifier = ProcessIdentifier.parse(TEST_SERVICE_VALUE);
        } catch (PeppolParsingException e) {
            return null;
        }

        Endpoint endpoint = Endpoint.of(TransportProfile.of(transportProfileAS4), URI.create(ADDRESS), testData);

        List<ProcessMetadata<Endpoint>> processes = new ArrayList<>();
        ProcessMetadata<Endpoint> process = ProcessMetadata.of(processIdentifier, endpoint);
        processes.add(process);

        ServiceMetadata sm = ServiceMetadata.of(ServiceInformation.of(null, null, processes));
        return sm;
    }

    @Test
    public void testGetDocumentTypeIdentifierWithScheme() throws PeppolParsingException {
        String documentId = "busdox-docid-qns::urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.peppol.eu:bis:peppol5a:ver2.0::2.1";
        dynamicDiscoveryServicePEPPOL.getDocumentTypeIdentifier(documentId);

        new Verifications() {{
            DocumentTypeIdentifier.parse(documentId);
        }};
    }

    @Test
    public void testGetDocumentTypeIdentifier() throws PeppolParsingException {
        String documentId = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:www.cenbii.eu:transaction:biitrns010:ver2.0:extended:urn:www.peppol.eu:bis:peppol5a:ver2.0::2.1";
        dynamicDiscoveryServicePEPPOL.getDocumentTypeIdentifier(documentId);

        new Verifications() {{
            DocumentTypeIdentifier.of(documentId);
        }};
    }

    @Test
    public void testGeProcessIdentifierWithScheme() throws PeppolParsingException {
        String processId = "cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii05:ver2.0";
        dynamicDiscoveryServicePEPPOL.getProcessIdentifier(processId);

        new Verifications() {{
            ProcessIdentifier.parse(processId);
        }};
    }

    @Test
    public void testGeProcessIdentifier() throws PeppolParsingException {
        String processId = "urn:www.cenbii.eu:profile:bii05:ver2.0";
        dynamicDiscoveryServicePEPPOL.getProcessIdentifier(processId);

        new Verifications() {{
            ProcessIdentifier.of(processId);
        }};
    }

    @Test(expected = ConfigurationException.class)
    public void testSmlZoneEmpty() throws EbMS3Exception {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SMLZONE);
            result = "";
            times = 1;
        }};
        dynamicDiscoveryServicePEPPOL.lookupInformation(DOMAIN, TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
    }

    @Test(expected = ConfigurationException.class)
    public void testSmlZoneNull() throws EbMS3Exception {
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SMLZONE);
            result = null;
            times = 1;
        }};
        dynamicDiscoveryServicePEPPOL.lookupInformation(DOMAIN, TEST_RECEIVER_ID, TEST_RECEIVER_ID_TYPE, TEST_ACTION_VALUE, TEST_SERVICE_VALUE, TEST_SERVICE_TYPE);
    }

}
