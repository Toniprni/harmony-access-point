package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.X509CertificateService;
import eu.domibus.core.proxy.ProxyUtil;
import eu.domibus.test.common.PKIUtil;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.common.lang.PeppolParsingException;
import network.oxalis.vefa.peppol.common.model.*;
import network.oxalis.vefa.peppol.lookup.api.LookupException;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.math.BigInteger;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.*;

@Configuration
public class DynamicDiscoveryServicePEPPOLConfigurationMockup {

    public static final Domain DOMAIN = new Domain("default", "default");

    public static final String FINAL_RECIPIENT1 = "0208:1111";
    public static final String FINAL_RECIPIENT2 = "0208:2222";
    public static final String FINAL_RECIPIENT3 = "0208:3333";
    public static final String FINAL_RECIPIENT4 = "0208:4444";

    private static final String PARTY_NAME1 = "party1";
    private static final Long PARTY_NAME1_CERTIFICATE_SERIAL_NUMBER = 100L;
    private static final String PARTY_NAME2 = "party2";
    private static final String PARTY_NAME3 = "party3";
    private static final Long PARTY_NAME2_CERTIFICATE_SERIAL_NUMBER = 200L;

    public static Map<String, FinalRecipientConfiguration> participantConfigurations = new HashMap<>();

    @Bean
    public Domain myDomain() {
        return DOMAIN;
    }


    @Primary
    @Bean
    public DynamicDiscoveryServicePEPPOL dynamicDiscoveryServicePEPPOL(DomibusPropertyProvider domibusPropertyProvider,
                                                                       MultiDomainCryptoService multiDomainCertificateProvider,
                                                                       DomainContextProvider domainProvider,
                                                                       ProxyUtil proxyUtil,
                                                                       CertificateService certificateService,
                                                                       DomibusHttpRoutePlanner domibusHttpRoutePlanner,
                                                                       X509CertificateService x509CertificateService,
                                                                       ObjectProvider<DomibusCertificateValidator> domibusCertificateValidators,
                                                                       ObjectProvider<DomibusBusdoxLocator> busdoxLocators,
                                                                       ObjectProvider<DomibusApacheFetcher> domibusApacheFetchers,
                                                                       ObjectProvider<EndpointInfo> endpointInfos,
                                                                       DynamicDiscoveryUtil dynamicDiscoveryUtil) {
        PKIUtil pkiUtil = new PKIUtil();
        //we create the certificate for party 1 and party 2 Access Points
        final X509Certificate party1Certificate = pkiUtil.createCertificateWithSubject(BigInteger.valueOf(PARTY_NAME1_CERTIFICATE_SERIAL_NUMBER), "CN=" + PARTY_NAME1 + ",OU=Domibus,O=eDelivery,C=EU");
        final X509Certificate party2Certificate = pkiUtil.createCertificateWithSubject(BigInteger.valueOf(PARTY_NAME2_CERTIFICATE_SERIAL_NUMBER), "CN=" + PARTY_NAME2 + ",OU=Domibus,O=eDelivery,C=EU");
        final X509Certificate expiredCertificate = pkiUtil.createCertificateWithSubject(BigInteger.valueOf(300L), "CN=" + PARTY_NAME3 + ",OU=Domibus,O=eDelivery,C=EU", DateUtils.addDays(Calendar.getInstance().getTime(), -30), DateUtils.addDays(Calendar.getInstance().getTime(), -20));

        //final recipient 1 and 2 are configured on party1 Access Point
        addParticipantConfiguration(FINAL_RECIPIENT1, PARTY_NAME1, party1Certificate);
        addParticipantConfiguration(FINAL_RECIPIENT2, PARTY_NAME1, party1Certificate);

        //final recipient 3 is configured on party2 Access Point
        addParticipantConfiguration(FINAL_RECIPIENT3, PARTY_NAME2, party2Certificate);

        //final recipient 4 is configured on party2 Access Point; certificate is expired
        addParticipantConfiguration(FINAL_RECIPIENT4, PARTY_NAME3, expiredCertificate);

        return new DynamicDiscoveryServicePEPPOL(domibusPropertyProvider,
                multiDomainCertificateProvider,
                domainProvider,
                proxyUtil,
                certificateService,
                domibusHttpRoutePlanner,
                x509CertificateService,
                domibusCertificateValidators,
                busdoxLocators,
                domibusApacheFetchers,
                endpointInfos,
                dynamicDiscoveryUtil) {
            @Override
            protected ServiceMetadata getServiceMetadata(
                    String finalRecipient,
                    String finalRecipientScheme,
                    String documentId,
                    String smlInfo,
                    String mode,
                    DomibusCertificateValidator domibusSMPCertificateValidator) throws PeppolLoadingException, LookupException, PeppolSecurityException, PeppolParsingException {
                final FinalRecipientConfiguration configuration = participantConfigurations.get(finalRecipient);
                if (configuration == null) {
                    throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Could not find the final recipient configuration for final recipient [" + finalRecipient + "]");
                }
                return configuration.getServiceMetadata();
            }

        };
    }


    private static void addParticipantConfiguration(String finalRecipient, String partyName, X509Certificate certificate) {
        ServiceMetadata serviceMetadata = buildServiceMetadata(partyName, certificate, finalRecipient);
        FinalRecipientConfiguration configuration = new FinalRecipientConfiguration(certificate, serviceMetadata, partyName);
        participantConfigurations.put(finalRecipient, configuration);
    }

    private static ServiceMetadata buildServiceMetadata(String partyName, X509Certificate certificate, String finalRecipient) {
        ProcessIdentifier processIdentifier;
        try {
            processIdentifier = ProcessIdentifier.parse("cenbii-procid-ubl::bdx:noprocess");
        } catch (PeppolParsingException e) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_001, "Could not parse process identifier", e);
        }

        Endpoint endpoint = Endpoint.of(TransportProfile.of(TransportProfile.AS4.getIdentifier()), URI.create(getAccessPointUrl(partyName)), certificate);

        List<ProcessMetadata<Endpoint>> processes = new ArrayList<>();
        ProcessMetadata<Endpoint> process = ProcessMetadata.of(processIdentifier, endpoint);
        processes.add(process);

        final ParticipantIdentifier participantIdentifier = ParticipantIdentifier.of(finalRecipient);

        return ServiceMetadata.of(ServiceInformation.of(participantIdentifier, null, processes));
    }

    private static String getAccessPointUrl(String partyName) {
        return "http://localhost:9090/" + partyName + "/msh";
    }
}
