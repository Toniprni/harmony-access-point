package eu.domibus.core.monitoring;

import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.message.testservice.TestService;
import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.messaging.MessagingProcessingException;
import eu.domibus.web.rest.ro.ConnectionMonitorRO;
import eu.domibus.web.rest.ro.TestServiceMessageInfoRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_MONITORING_CONNECTION_PARTY_ENABLED;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
@RunWith(JMockit.class)
public class ConnectionMonitoringServiceImplTest {

    @Tested
    ConnectionMonitoringServiceImpl connectionMonitoringService;

    @Injectable
    PartyService partyService;

    @Injectable
    TestService testService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void isMonitoringEnabledFalse() {
        new Expectations() {{
            domibusPropertyProvider.getStringListProperty(DOMIBUS_MONITORING_CONNECTION_PARTY_ENABLED);
            result = new ArrayList<>();
        }};

        boolean res = connectionMonitoringService.isMonitoringEnabled();
        Assert.assertFalse(res);
    }

    @Test
    public void isMonitoringEnabledTrue() {
        new Expectations() {{
            domibusPropertyProvider.getStringListProperty(DOMIBUS_MONITORING_CONNECTION_PARTY_ENABLED);
            result = Arrays.asList("blue_gw");

            partyService.getGatewayPartyIdentifier();
            result = "my_gw";
        }};

        boolean res2 = connectionMonitoringService.isMonitoringEnabled();
        Assert.assertTrue(res2);
    }

    @Test
    public void isMonitoringEnabledAlsoFalse() {
        new Expectations() {{
            domibusPropertyProvider.getStringListProperty(DOMIBUS_MONITORING_CONNECTION_PARTY_ENABLED);
            result = Arrays.asList("blue_gw");

            partyService.getGatewayPartyIdentifier();
            result = "blue_gw";
        }};

        boolean res2 = connectionMonitoringService.isMonitoringEnabled();
        Assert.assertFalse(res2);
    }

    @Test
    public void sendTestMessages_NotApplicable() throws IOException, MessagingProcessingException {
        String selfParty = "self";
        String partyId2 = "partyId2";

        new Expectations(connectionMonitoringService) {{
            partyService.findPushToPartyNamesForTest();
            result = Arrays.asList(selfParty);

            partyService.getGatewayPartyIdentifier();
            result = selfParty;

            connectionMonitoringService.getAllMonitoredPartiesButMyself((List<String>)any, anyString);
            result = new ArrayList<>();

        }};

        connectionMonitoringService.sendTestMessages();

        new Verifications() {{
            testService.submitTest(selfParty, partyId2);
            times = 0;
        }};
    }

    @Test
    public void sendTestMessages_NotEnabled() throws IOException, MessagingProcessingException {
        String selfParty = "self";
        String partyId2 = "partyId2";

        new Expectations(connectionMonitoringService) {{
            partyService.findPushToPartyNamesForTest();
            result = Arrays.asList(selfParty, partyId2);

            partyService.getGatewayPartyIdentifier();
            result = selfParty;

            connectionMonitoringService.getAllMonitoredPartiesButMyself((List<String>)any, anyString);
            result = new ArrayList<>();
        }};

        connectionMonitoringService.sendTestMessages();

        new Verifications() {{
            testService.submitTest(selfParty, partyId2);
            times = 0;
        }};
    }

    @Test
    public void sendTestMessages() throws IOException, MessagingProcessingException {
        String selfParty = "self";
        String partyId2 = "partyId2";

        new Expectations(connectionMonitoringService) {{
            partyService.findPushToPartyNamesForTest();
            result = Arrays.asList(selfParty, partyId2);

            partyService.getGatewayPartyIdentifier();
            result = selfParty;

            connectionMonitoringService.getAllMonitoredPartiesButMyself((List<String>)any, anyString);
            result = Arrays.asList(partyId2);

            testService.submitTest(selfParty, partyId2);
            result = "testMessageId";
        }};

        connectionMonitoringService.sendTestMessages();

        new Verifications() {{
            testService.submitTest(selfParty, partyId2);
            times = 1;
        }};
    }

    @Test
    public void testGetConnectionStatus() {
        // Given
        String partyId1 = "partyId1";
        String partyId2 = "partyId2";
        String[] partyIds = {partyId1, partyId2};

        TestServiceMessageInfoRO lastSent1 = new TestServiceMessageInfoRO() {{
            setMessageStatus(MessageStatus.ACKNOWLEDGED);
        }};
        TestServiceMessageInfoRO lastReceived1 = new TestServiceMessageInfoRO() {{
            setMessageStatus(MessageStatus.ACKNOWLEDGED);
        }};

        TestServiceMessageInfoRO lastSent2 = new TestServiceMessageInfoRO() {{
            setMessageStatus(MessageStatus.SEND_FAILURE);
        }};
        TestServiceMessageInfoRO lastReceived2 = new TestServiceMessageInfoRO() {{
            setMessageStatus(MessageStatus.ACKNOWLEDGED);
        }};

        new Expectations() {{
            testService.getLastTestSent(partyId1);
            result = lastSent1;

            testService.getLastTestReceived(partyId1, null);
            result = lastReceived1;

            testService.getLastTestSent(partyId2);
            result =lastSent2;

            testService.getLastTestReceived(partyId2, null);
            result = lastReceived2;

            partyService.findPushToPartyNamesForTest();
            result = Arrays.asList(partyId1, partyId2);

            domibusPropertyProvider.getStringListProperty(DOMIBUS_MONITORING_CONNECTION_PARTY_ENABLED);
            result = Arrays.asList(partyId1);
        }};

        // When
        Map<String, ConnectionMonitorRO> result = connectionMonitoringService.getConnectionStatus(partyIds);

        // Then
        Assert.assertEquals(result.size(), 2);

        Assert.assertEquals(result.get(partyId1).isTestable(), true);
        Assert.assertEquals(result.get(partyId1).isMonitored(), true);
        Assert.assertEquals(result.get(partyId1).getStatus(), ConnectionMonitorRO.ConnectionStatus.OK);

        Assert.assertEquals(result.get(partyId2).isTestable(), true);
        Assert.assertEquals(result.get(partyId2).isMonitored(), false);
        Assert.assertEquals(result.get(partyId2).getStatus(), ConnectionMonitorRO.ConnectionStatus.BROKEN);

    }
}
