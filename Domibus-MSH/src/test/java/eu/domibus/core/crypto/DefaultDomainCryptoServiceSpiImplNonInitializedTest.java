package eu.domibus.core.crypto;

import eu.domibus.api.cluster.SignalService;
import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.exception.ConfigurationException;
import eu.domibus.core.util.backup.BackupService;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

/**
 * Tests that use a non-initialized SUT (the #init() method is intentionally stubbed out).
 *
 * @author Sebastian-Ion TINCU
 */
@RunWith(JMockit.class)
public class DefaultDomainCryptoServiceSpiImplNonInitializedTest {

    public static final String PRIVATE_KEY_PASSWORD = "privateKeyPassword";

    public static final String TRUST_STORE_PASSWORD = "trustStorePassword";

    public static final String TRUST_STORE_TYPE = "trustStoreType";

    public static final String TRUST_STORE_LOCATION = "trustStoreLocation";

    @Tested
    private DefaultDomainCryptoServiceSpiImpl domainCryptoService;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected CertificateService certificateService;

    @Injectable
    protected SignalService signalService;

    @Injectable
    private X509Certificate x509Certificate;

    @Injectable
    private Domain domain;

    @Injectable
    private KeyStore trustStore;

    @Injectable
    private DomibusCoreMapper coreMapper;

    @Injectable
    private BackupService backupService;

    @Injectable
    DomainTaskExecutor domainTaskExecutor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        new MockUp<DefaultDomainCryptoServiceSpiImpl>() {
            @Mock
            void init() { /* avoid @PostConstruct initialization */ }
        };

        new NonStrictExpectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_TYPE);
            result = "keystoreType";
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_PASSWORD);
            result = "keystorePassword";
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS);
            result = "privateKeyAlias";
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_PASSWORD);
            result = PRIVATE_KEY_PASSWORD;
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_LOCATION);
            result = "keystoreLocation";

            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_LOCATION);
            result = TRUST_STORE_LOCATION;
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD);
            result = "trustStorePassword";
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_TYPE);
            result = TRUST_STORE_TYPE;
        }};
    }

    @Test
    public void returnsTheCorrectValidityOfTheCertificateChain() {
        // Given
        Deencapsulation.setField(domainCryptoService, "truststore", trustStore);

        new Expectations() {{
            certificateService.isCertificateChainValid(trustStore, "alias");
            result = true;
        }};

        // When
        boolean valid = domainCryptoService.isCertificateChainValid("alias");

        // Then
        Assert.assertTrue("Should have correctly returned the validity of the certificate chain", valid);
    }

    @Test
    public void returnsTheCorrectTrustStoreWhenLoadingIt(@Injectable InputStream trustStoreInputStream) {
        // Given
        new MockUp<DefaultDomainCryptoServiceSpiImpl>() {
            @Mock
            InputStream loadInputStream(ClassLoader classLoader, String trustStoreLocation) {
                return trustStoreInputStream;
            }

            @Mock
            String decryptPassword(String password, PasswordEncryptor passwordEncryptor) {
                return "decryptedPassword";
            }

            @Mock
            KeyStore load(InputStream input, String storepass, String provider, String type) {
                return trustStore;
            }
        };

        // When
        KeyStore result = domainCryptoService.loadTrustStore();

        // Then
        Assert.assertEquals("Should have returned the correct trust store when loading it", trustStore, result);
    }

    @Test
    public void refreshesTheTrustStoreWithTheLoadedTrustStore() {
        // Given
        new MockUp<DefaultDomainCryptoServiceSpiImpl>() {
            @Mock
            KeyStore loadTrustStore() {
                return trustStore;
            }
        };

        // When
        domainCryptoService.refreshTrustStore();

        // Then
        new Verifications() {{
            domainCryptoService.setTrustStore(trustStore);
        }};
    }

    @Test
    public void getKeystoreProperties_missingKeystoreTypePropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_TYPE);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the keystore properties for domain");

        // When
        domainCryptoService.getKeystoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingKeystorePasswordPropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_PASSWORD);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the keystore properties for domain");

        // When
        domainCryptoService.getKeystoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingKeystorePrivateKeyAliasPropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEY_PRIVATE_ALIAS);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the keystore properties for domain");

        // When
        domainCryptoService.getKeystoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingKeystoreLocationPropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_KEYSTORE_TYPE);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the keystore properties for domain");

        // When
        domainCryptoService.getKeystoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingTruststoreTypePropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_TYPE);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the truststore properties for domain");

        // When
        domainCryptoService.getTrustStoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingTruststorePasswordPropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the truststore properties for domain");

        // When
        domainCryptoService.getTrustStoreProperties();
    }

    @Test
    public void getKeystoreProperties_missingTruststoreLocationPropertyConfigurationException() {
        // Given
        new Expectations() {{
            domibusPropertyProvider.getProperty(domain, DOMIBUS_SECURITY_TRUSTSTORE_PASSWORD);
            result = null;
        }};
        thrown.expect(ConfigurationException.class);
        thrown.expectMessage("Error while trying to load the truststore properties for domain");

        // When
        domainCryptoService.getTrustStoreProperties();
    }

}
