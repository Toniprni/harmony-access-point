package eu.domibus.core.certificate;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.security.AuthRole;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.core.crypto.MultiDomainCryptoServiceImpl;
import eu.domibus.core.scheduler.DomibusQuartzJobBean;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.KeyStore;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@DisallowConcurrentExecution
public class SaveCertificateAndLogRevocationJob extends DomibusQuartzJobBean {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SaveCertificateAndLogRevocationJob.class);

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private MultiDomainCryptoServiceImpl multiDomainCertificateProvider;

    @Override
    protected void executeJob(JobExecutionContext context, Domain domain) {
        LOG.info("Checking certificate expiration");
        try {
            final KeyStore trustStore = multiDomainCertificateProvider.getTrustStore(domain);
            final KeyStore keyStore = multiDomainCertificateProvider.getKeyStore(domain);
            certificateService.saveCertificateAndLogRevocation(trustStore, keyStore);

            certificateService.sendCertificateAlerts();
        } catch (eu.domibus.api.security.CertificateException ex) {
            LOG.warn("An problem occurred while loading keystore:[{}]", ex.getMessage(), ex);
        }
    }

    @Override
    public void setQuartzJobSecurityContext() {
        authUtils.setAuthenticationToSecurityContext(DOMIBUS_QUARTZ_USER, DOMIBUS_QUARTZ_PASSWORD, AuthRole.ROLE_AP_ADMIN);
    }
}
