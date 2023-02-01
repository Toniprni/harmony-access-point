package eu.domibus.core.crypto;

import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.pki.*;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.crypto.api.DomainCryptoService;
import eu.domibus.core.crypto.spi.CertificateEntrySpi;
import eu.domibus.core.crypto.spi.DomainCryptoServiceSpi;
import eu.domibus.core.crypto.spi.DomainSpi;
import eu.domibus.core.crypto.spi.KeyStoreContentInfoSpi;
import eu.domibus.core.crypto.spi.model.AuthenticationError;
import eu.domibus.core.crypto.spi.model.AuthenticationException;
import eu.domibus.core.ebms3.EbMS3ExceptionBuilder;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.ws.WebServiceException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.crypto.spi.AbstractCryptoServiceSpi.DEFAULT_AUTHENTICATION_SPI;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public class DomainCryptoServiceImpl implements DomainCryptoService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomainCryptoServiceImpl.class);

    protected static final String IAM_AUTHENTICATION_IDENTIFIER = DOMIBUS_EXTENSION_IAM_AUTHENTICATION_IDENTIFIER;

    private DomainCryptoServiceSpi iamProvider;

    private Domain domain;

    protected List<DomainCryptoServiceSpi> domainCryptoServiceSpiList;

    protected DomibusPropertyProvider domibusPropertyProvider;

    protected CertificateService certificateService;

    private final KeystorePersistenceService keystorePersistenceService;

    protected final DomibusCoreMapper coreMapper;

    public DomainCryptoServiceImpl(Domain domain,
                                   List<DomainCryptoServiceSpi> domainCryptoServiceSpiList,
                                   DomibusPropertyProvider domibusPropertyProvider,
                                   CertificateService certificateService,
                                   KeystorePersistenceService keystorePersistenceService,
                                   DomibusCoreMapper coreMapper) {
        this.domain = domain;
        this.domainCryptoServiceSpiList = domainCryptoServiceSpiList;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.certificateService = certificateService;
        this.keystorePersistenceService = keystorePersistenceService;
        this.coreMapper = coreMapper;
    }

    public void init() {
        getIAMProvider();
        iamProvider.init();
    }

    @Override
    public X509Certificate getCertificateFromKeyStore(String alias) throws KeyStoreException {
        return iamProvider.getCertificateFromKeyStore(alias);
    }

    @Override
    public X509Certificate getCertificateFromTrustStore(String alias) throws KeyStoreException {
        return iamProvider.getCertificateFromTrustStore(alias);
    }

    @Override
    public X509Certificate[] getX509Certificates(CryptoType cryptoType) throws WSSecurityException {
        return iamProvider.getX509Certificates(cryptoType);
    }

    @Override
    public String getX509Identifier(X509Certificate cert) throws WSSecurityException {
        return iamProvider.getX509Identifier(cert);
    }

    @Override
    public PrivateKey getPrivateKey(X509Certificate certificate, CallbackHandler callbackHandler) {
        try {
            return iamProvider.getPrivateKey(certificate, callbackHandler);
        } catch (WSSecurityException e) {
            try {
                LOG.info("Error when trying to retrieve the private key for certificate [{}]: [{}]", certificate, e);
                String identifier = iamProvider.getX509Identifier(certificate);
                throw new DomibusCertificateException("Could not retrieve private key for alias: " + identifier, e);
            } catch (WSSecurityException ex) {
                throw new DomibusCertificateException("Could not retrieve X509 identifier for certificate: " + certificate, ex);
            }
        }
    }

    @Override
    public PrivateKey getPrivateKey(PublicKey publicKey, CallbackHandler callbackHandler) throws WSSecurityException {
        return iamProvider.getPrivateKey(publicKey, callbackHandler);
    }

    @Override
    public PrivateKey getPrivateKey(String identifier, String password) throws WSSecurityException {
        return iamProvider.getPrivateKey(identifier, password);
    }

    @Override
    public void verifyTrust(PublicKey publicKey) throws WSSecurityException {
        iamProvider.verifyTrust(publicKey);
    }

    @Override
    public void verifyTrust(X509Certificate[] certs, boolean enableRevocation, Collection<Pattern> subjectCertConstraints, Collection<Pattern> issuerCertConstraints) throws WSSecurityException {
        try {
            iamProvider.verifyTrust(certs, enableRevocation, subjectCertConstraints, issuerCertConstraints);
        } catch (AuthenticationException e) {
            if (e.getCause() != null) {
                throw e;
            }
            LOG.error("Certificate validation error", e);
            AuthenticationError authenticationError = e.getAuthenticationError();
            switch (authenticationError) {
                case EBMS_0101:
                    throw new WebServiceException(EbMS3ExceptionBuilder.getInstance()
                            .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0101)
                            .message("Sender certificate is not valid or has been revoked")
                            .refToMessageId(LOG.getMDC(DomibusLogger.MDC_MESSAGE_ID))
                            .cause(e)
                            .mshRole(MSHRole.RECEIVING)
                            .build());
                default:
                    throw new WebServiceException(EbMS3ExceptionBuilder.getInstance()
                            .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0004)
                            .message("unknown error occurred")
                            .refToMessageId(LOG.getMDC(DomibusLogger.MDC_MESSAGE_ID))
                            .cause(e)
                            .build());
            }
        }
    }

    @Override
    public String getDefaultX509Identifier() throws WSSecurityException {
        return iamProvider.getDefaultX509Identifier();
    }

    @Override
    public String getPrivateKeyPassword(String alias) {
        return iamProvider.getPrivateKeyPassword(alias);
    }

//    @Override
//    public void refreshTrustStore() throws CryptoException {
//        iamProvider.refreshTrustStore();
//    }

//    @Override
//    public void refreshKeyStore() {
//        iamProvider.refreshKeyStore();
//    }

    @Override
    public void replaceTrustStore(byte[] storeContent, String storeFileName, String storePassword) throws CryptoException {
        iamProvider.replaceTrustStore(storeContent, storeFileName, storePassword);
    }

    @Override
    public void replaceTrustStore(KeyStoreContentInfo storeInfo) {
        KeyStoreContentInfoSpi keyStoreContentInfoSpi = coreMapper.keyStoreContentInfoToKeyStoreContentInfoSpi(storeInfo);
        iamProvider.replaceTrustStore(keyStoreContentInfoSpi);
    }

    @Override
    public void replaceKeyStore(KeyStoreContentInfo storeInfo) {
        KeyStoreContentInfoSpi keyStoreContentInfoSpi = coreMapper.keyStoreContentInfoToKeyStoreContentInfoSpi(storeInfo);
        iamProvider.replaceKeyStore(keyStoreContentInfoSpi);
    }

    @Override
    public KeyStore getKeyStore() {
        return iamProvider.getKeyStore();
    }

    @Override
    public KeyStore getTrustStore() {
        return iamProvider.getTrustStore();
    }


    @Override
    public boolean isCertificateChainValid(String alias) throws DomibusCertificateException {
        return iamProvider.isCertificateChainValid(alias);
    }

    @Override
    public boolean addCertificate(X509Certificate certificate, String alias, boolean overwrite) {
        return iamProvider.addCertificate(certificate, alias, overwrite);
    }

    @Override
    public void addCertificate(List<CertificateEntry> certificates, boolean overwrite) {
        List<CertificateEntrySpi> list = certificates.stream()
                .map(c -> new CertificateEntrySpi(c.getAlias(), c.getCertificate()))
                .collect(Collectors.toList());
        iamProvider.addCertificate(list, overwrite);
    }

    @Override
    public List<TrustStoreEntry> getKeyStoreEntries() {
        KeyStore store = iamProvider.getKeyStore();
        return certificateService.getStoreEntries(store);
    }

    @Override
    public KeyStoreContentInfo getKeyStoreContent() {
        KeyStore store = iamProvider.getKeyStore();
        KeystorePersistenceInfo persistenceInfo = keystorePersistenceService.getKeyStorePersistenceInfo();
        return certificateService.getStoreContent(store, persistenceInfo.getName(), persistenceInfo.getPassword());
    }

    @Override
    public List<TrustStoreEntry> getTrustStoreEntries() {
        KeyStore store = iamProvider.getTrustStore();
        return certificateService.getStoreEntries(store);
    }

    @Override
    public KeyStoreContentInfo getTrustStoreContent() {
        KeyStore store = iamProvider.getTrustStore();
        KeystorePersistenceInfo persistenceInfo = keystorePersistenceService.getTrustStorePersistenceInfo();
        return certificateService.getStoreContent(store, persistenceInfo.getName(), persistenceInfo.getPassword());
    }

    @Override
    public boolean removeCertificate(String alias) {
        return iamProvider.removeCertificate(alias);
    }

    @Override
    public void removeCertificate(List<String> aliases) {
        iamProvider.removeCertificate(aliases);
    }

    protected void setDomainCryptoServiceSpiList(List<DomainCryptoServiceSpi> domainCryptoServiceSpiList) {
        this.domainCryptoServiceSpiList = domainCryptoServiceSpiList;
    }

    @Override
    public void resetKeyStore() {
        getIAMProvider();
        iamProvider.resetKeyStore();
    }

    @Override
    public void resetTrustStore() {
        getIAMProvider();
        iamProvider.resetTrustStore();
    }

    @Override
    public void resetStores() {
        getIAMProvider();
        iamProvider.resetKeyStore();
        iamProvider.resetTrustStore();
    }

    @Override
    public void resetSecurityProfiles() {
        getIAMProvider();
        iamProvider.resetSecurityProfiles();
    }

    @Override
    public boolean isTrustStoreChangedOnDisk() {
        return iamProvider.isTrustStoreChangedOnDisk();
    }

    @Override
    public boolean isKeyStoreChangedOnDisk() {
        return iamProvider.isKeyStoreChangedOnDisk();
    }

    private void getIAMProvider() {
        String spiIdentifier = domibusPropertyProvider.getProperty(domain, IAM_AUTHENTICATION_IDENTIFIER);
        if (spiIdentifier.equals(DEFAULT_AUTHENTICATION_SPI) && domainCryptoServiceSpiList.size() > 1) {
            LOG.warn("A custom authentication implementation has been provided but property:[{}}] is configured with default value:[{}]",
                    DOMIBUS_EXTENSION_IAM_AUTHENTICATION_IDENTIFIER, spiIdentifier);
        }
        final List<DomainCryptoServiceSpi> providerList = domainCryptoServiceSpiList.stream().
                filter(domainCryptoServiceSpi -> spiIdentifier.equals(domainCryptoServiceSpi.getIdentifier())).
                collect(Collectors.toList());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Authentication spi:");
            providerList.forEach(domainCryptoServiceSpi -> LOG.debug(" identifier:[{}] for class:[{}]", domainCryptoServiceSpi.getIdentifier(), domainCryptoServiceSpi.getClass()));
        }

        if (providerList.size() > 1) {
            throw new IllegalStateException(String.format("More than one authentication service provider for identifier:[%s]", spiIdentifier));
        }
        if (providerList.isEmpty()) {
            throw new IllegalStateException(String.format("No authentication service provider found for given identifier:[%s]", spiIdentifier));
        }

        iamProvider = providerList.get(0);
        iamProvider.setDomain(new DomainSpi(domain.getCode(), domain.getName()));

        LOG.info("Active IAM provider identifier:[{}] for domain:[{}]", iamProvider.getIdentifier(), domain.getName());
    }

}
