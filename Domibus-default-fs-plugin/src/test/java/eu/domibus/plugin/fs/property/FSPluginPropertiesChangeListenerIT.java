package eu.domibus.plugin.fs.property;

import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusSchedulerExtService;
import eu.domibus.plugin.fs.property.listeners.OutQueueConcurrencyChangeListener;
import eu.domibus.plugin.fs.property.listeners.TriggerChangeListener;
import eu.domibus.plugin.fs.queue.FSSendMessageListenerContainer;
import eu.domibus.test.AbstractIT;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static eu.domibus.plugin.fs.property.FSPluginPropertiesMetadataManagerImpl.*;

/**
 * @author Ion Perpegel
 * @author Catalin Enache
 * @since 4.2
 */
public class FSPluginPropertiesChangeListenerIT extends AbstractIT {

    @Autowired
    private DomibusSchedulerExtService domibusSchedulerExt;

    @Autowired
    private FSSendMessageListenerContainer messageListenerContainer;

    @Autowired
    private OutQueueConcurrencyChangeListener outQueueConcurrencyChangeListener;

    @Autowired
    private TriggerChangeListener triggerChangeListener;

    @Autowired
    private DomainExtService domainExtService;

    @Configuration
    static class ContextConfiguration {

        @Primary
        @Bean
        public DomibusSchedulerExtService domibusSchedulerExt() {
            return Mockito.mock(DomibusSchedulerExtService.class);
        }

        @Primary
        @Bean
        public FSSendMessageListenerContainer messageListenerContainer() {
            return Mockito.mock(FSSendMessageListenerContainer.class);
        }

    }


    @Test
    public void testTriggerChangeListener() {
        boolean handlesWorkerInterval = triggerChangeListener.handlesProperty(SEND_WORKER_INTERVAL);
        Assert.assertTrue(handlesWorkerInterval);

        try {
            triggerChangeListener.propertyValueChanged("default", SEND_WORKER_INTERVAL, "wrong-value");
            Assert.fail("Expected exception not raised");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid"));
        }

        triggerChangeListener.propertyValueChanged("default", SEND_WORKER_INTERVAL, "3000");
        triggerChangeListener.propertyValueChanged("default", SENT_PURGE_WORKER_CRONEXPRESSION, "0 0/15 * * * ?");

        Mockito.verify(domibusSchedulerExt, Mockito.times(1)).rescheduleJob("default", "fsPluginSendMessagesWorkerJob", 3000);
        Mockito.verify(domibusSchedulerExt, Mockito.times(1)).rescheduleJob("default", "fsPluginPurgeSentWorkerJob", "0 0/15 * * * ?");
    }

    @Test
    public void testOutQueueConcurrencyChangeListener() {

        boolean handlesProperty = outQueueConcurrencyChangeListener.handlesProperty(OUT_QUEUE_CONCURRENCY);
        Assert.assertTrue(handlesProperty);

        DomainDTO aDefault = domainExtService.getDomain("default");

        outQueueConcurrencyChangeListener.propertyValueChanged("default", OUT_QUEUE_CONCURRENCY, "1-2");
        Mockito.verify(messageListenerContainer, Mockito.times(1)).updateMessageListenerContainerConcurrency(aDefault, "1-2");
    }
}