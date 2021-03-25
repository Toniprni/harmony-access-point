package eu.domibus.core.crypto.spi.dss;

import mockit.*;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import static org.junit.Assert.*;

/**
 * @author Thomas Dussart
 * @since 4.2
 */
public class DomibusDataLoaderTest {

  /*  @Test
    public void mergeCustomTlsTrustStoreWithCacert(
            @Mocked KeyStore customTlsTrustStore,
            @Mocked KeyStore cacertTrustStore,
            @Mocked Enumeration enumeration,
            @Mocked Certificate cert) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        final String cacertAlias = "cacertAlias";
        new Expectations(dssConfiguration) {{
            KeyStore.getInstance("${domibus.dss.ssl.trust.store.type}");
            result = customTlsTrustStore;
            new MockUp<FileInputStream>() {
                @Mock
                void $init(String fileName) {
                }

                @Mock
                int read() {
                    return 123;
                }

                @Mock
                public void close() throws IOException {
                    System.out.println("Closing file input stream");
                }

                ;
            };
            dssConfiguration.loadCacertTrustStore();
            result = cacertTrustStore;
            cacertTrustStore.aliases();
            result = enumeration;

            enumeration.hasMoreElements();
            returns(true, false);
            enumeration.nextElement();
            result = cacertAlias;
            cacertTrustStore.getCertificate(cacertAlias);
            result = cert;
        }};
        dssConfiguration.mergeCustomTlsTrustStoreWithCacert();
        new Verifications() {{
            customTlsTrustStore.setCertificateEntry(cacertAlias, cert);
            times = 1;
        }};
    }


    @Test
    public void loadCacertTrustStoreFromDefaultLocation(@Mocked KeyStore keyStore){
        final String cacertPath = "";
        Deencapsulation.setField(dssConfiguration,"cacertPath",cacertPath);
        final String cacertType = "cacertType";
        Deencapsulation.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        Deencapsulation.setField(dssConfiguration, cacertPassword, cacertPassword);

        new Expectations(dssConfiguration){{
            dssConfiguration.getJavaHome();
            result="\\home";
            dssConfiguration.loadKeystore(anyString, cacertType, cacertPassword);times=1;
            result=keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();

    }

    @Test
    public void loadCacertTrustStoreFromCustomLocation(@Mocked KeyStore keyStore){

        final String cacertPath = "cacertPath";
        ReflectionTestUtils.setField(dssConfiguration, cacertPath, cacertPath);
        final String cacertType = "cacertType";
        ReflectionTestUtils.setField(dssConfiguration, cacertType, cacertType);
        final String cacertPassword = "cacertPassword";
        ReflectionTestUtils.setField(dssConfiguration, cacertPassword, cacertPassword);
        new Expectations(dssConfiguration){{
            dssConfiguration.loadKeystore(cacertPath, cacertType, cacertPassword);times=1;
            result=keyStore;
        }};
        dssConfiguration.loadCacertTrustStore();
    }*/

}