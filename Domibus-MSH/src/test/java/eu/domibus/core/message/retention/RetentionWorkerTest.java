package eu.domibus.core.message.retention;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.pmode.ConfigurationDAO;
import eu.domibus.core.scheduler.DomibusQuartzJobBean;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

/**
 * @author Soumya Chandran
 * @since 5.0
 */
@RunWith(JMockit.class)
public class RetentionWorkerTest {

    @Tested
    RetentionWorker retentionWorker;

    @Injectable
    protected List<MessageRetentionService> messageRetentionServices;

    @Injectable
    private MessageRetentionDefaultService messageRetentionService;

    @Injectable
    private ConfigurationDAO configurationDAO;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    private DomainService domainService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private DatabaseUtil databaseUtil;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void executeJob(@Mocked JobExecutionContext context, @Mocked Domain domain) throws JobExecutionException {

        new Expectations() {{
            configurationDAO.configurationExists();
            result = true;
        }};

        retentionWorker.executeJob(context, domain);

        new FullVerifications() {{
            messageRetentionService.deleteExpiredMessages();
        }};
    }

    @Test
    public void setQuartzJobSecurityContext() {

        retentionWorker.setQuartzJobSecurityContext();

        new FullVerifications() {{
            authUtils.setAuthenticationToSecurityContext(DomibusQuartzJobBean.DOMIBUS_QUARTZ_USER, DomibusQuartzJobBean.DOMIBUS_QUARTZ_PASSWORD);
        }};
    }
}