package eu.domibus.plugin.ws.webservice;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusLoggerImpl;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

import static eu.domibus.api.multitenancy.DomainService.DEFAULT_DOMAIN;

/* Submit 20 invalid messages on 2 domains:
Average execution time with previous logging approach   5.690666666 seconds
Average execution time with new logging approach        5.629833333 seconds
*/
public class LoggingSegregationImpactOnSubmit extends SubmitMessageIT {
    public static final Domain ANOTHER_DOMAIN = new Domain("anotherDomain", "another domain");
    private static final DomibusLogger LOG = new DomibusLoggerImpl(LoggerFactory.getLogger(LoggingSegregationImpactOnSubmit.class));

    @Before
    public void beforeTest() throws Exception {
        getDurationForSubmittingMessages(5);
        setCurrentDomain(ANOTHER_DOMAIN);
    }

    private void setCurrentDomain(Domain domain) {
        LOG.putMDC(DomibusLogger.MDC_DOMAIN, domain.getCode());
    }

    @Test
    public void test() throws Exception {

        DomibusLoggerFactory.USE_PREVIOUS_LOGGING_APPROACH = true;
        Duration duration = getAverageDurationOverNrIterations();
        System.out.println("Average execution time with previous logging approach " + duration);

        DomibusLoggerFactory.USE_PREVIOUS_LOGGING_APPROACH = false;
        duration = duration = getAverageDurationOverNrIterations();
        System.out.println("Average execution time with new logging approach " + duration);
    }

    private Duration getAverageDurationOverNrIterations() throws Exception {
        int nrMessages = 10;
        int nrIterations = 3;

        Duration duration = Duration.ZERO;
        setCurrentDomain(DEFAULT_DOMAIN);
        for (int i = 0; i < nrIterations; i++) {
            duration = duration.plus(getDurationForSubmittingMessages(nrMessages));
        }
        setCurrentDomain(ANOTHER_DOMAIN);
        for (int i = 0; i < nrIterations; i++) {
            duration = duration.plus(getDurationForSubmittingMessages(nrMessages));
        }
        duration = duration.dividedBy(nrIterations*2);
        return duration;
    }

    private Duration getDurationForSubmittingMessages(int nrMessages) throws Exception {
        Instant beforeExecution = Instant.now();
        for (int i = 0; i < nrMessages; i++) {
            super.testSubmitMessageValid();
        }
        Instant afterExecution = Instant.now();
        return Duration.between(beforeExecution, afterExecution);
    }
}
