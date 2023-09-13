package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.AbstractIT;
import eu.domibus.api.dynamicdyscovery.DynamicDiscoveryCertificateEntity;
import eu.domibus.api.model.*;
import eu.domibus.api.model.participant.FinalRecipientEntity;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.AuthenticationException;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.participant.FinalRecipientDao;
import eu.domibus.core.pmode.provider.FinalRecipientService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.ProcessingType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.security.KeyStoreException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY;
import static eu.domibus.core.pmode.provider.dynamicdiscovery.DynamicDiscoveryServicePEPPOLConfigurationMockup.*;
import static org.junit.Assert.*;

/**
 * @author Cosmin Baciu
 * @since 5.1.1
 */
public class DynamicDiscoveryServiceTestIT extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DynamicDiscoveryServiceTestIT.class);

    @Autowired
    DynamicDiscoveryPModeProvider dynamicDiscoveryPModeProvider;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    MultiDomainCryptoService multiDomainCertificateProvider;

    @Autowired
    FinalRecipientService finalRecipientService;

    @Autowired
    FinalRecipientDao finalRecipientDao;

    @Autowired
    DynamicDiscoveryCertificateDao dynamicDiscoveryCertificateDao;

    @Autowired
    DynamicDiscoveryCertificateService dynamicDiscoveryCertificateService;

    @Configuration
    static class ContextConfiguration {

    }

    @Before
    public void setUp() throws Exception {
        uploadPmode(SERVICE_PORT, "dataset/pmode/PModeDynamicDiscovery.xml", null);

        domibusPropertyProvider.setProperty(DOMAIN, DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY, "true");
        domibusPropertyProvider.setProperty(DOMAIN, DomibusPropertyMetadataManagerSPI.DOMIBUS_DEPLOYMENT_CLUSTERED, "true");

        domibusPropertyProvider.setProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_DYNAMICDISCOVERY_CLIENT_SPECIFICATION, DynamicDiscoveryClientSpecification.PEPPOL.getName());
        dynamicDiscoveryPModeProvider.init();
    }

    @After
    public void clean() {
        domibusPropertyProvider.setProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_DYNAMICDISCOVERY_CLIENT_SPECIFICATION, DynamicDiscoveryClientSpecification.OASIS.getName());
        domibusPropertyProvider.setProperty(DOMAIN, DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY, "false");
        domibusPropertyProvider.setProperty(DOMAIN, DomibusPropertyMetadataManagerSPI.DOMIBUS_DEPLOYMENT_CLUSTERED, "false");
    }

    //start tests

    @Test
    public void lookupAndUpdateConfigurationForToPartyId() throws EbMS3Exception {
        //clean up
        cleanBeforeLookup();

        final String finalRecipient1PartyName = DynamicDiscoveryServicePEPPOLConfigurationMockup.participantConfigurations.get(FINAL_RECIPIENT1).getPartyName();
        LOG.info("---first lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT1, finalRecipient1PartyName);
        //we expect the party and the certificate are added
        doLookupForFinalRecipient(FINAL_RECIPIENT1, finalRecipient1PartyName, 3, 2, 1, 1);

        //we expect that one entry is added in the database for the first lookup
        final DynamicDiscoveryCertificateEntity finalRecipient1PartyEntityFirstLookup = dynamicDiscoveryCertificateDao.findByCommonName(participantConfigurations.get(FINAL_RECIPIENT1).getPartyName());
        assertNotNull(finalRecipient1PartyEntityFirstLookup);
        assertEquals(PARTY_NAME1, finalRecipient1PartyEntityFirstLookup.getCn());
        final Date dynamicDiscoveryTimeFirstLookup = finalRecipient1PartyEntityFirstLookup.getDynamicDiscoveryTime();
        assertNotNull(dynamicDiscoveryTimeFirstLookup);
        assertNotNull(finalRecipient1PartyEntityFirstLookup.getFingerprint());
        assertNotNull(finalRecipient1PartyEntityFirstLookup.getSubject());
        assertNotNull(finalRecipient1PartyEntityFirstLookup.getSerial());

        try {
            //sleep 100 milliseconds so that the second DDC lookup time is after the first lookup
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error sleeping thread", e);
        }

        LOG.info("---second lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT1, finalRecipient1PartyName);
        //we expect that no new certificates are added in the truststore and no new parties are added in the Pmode
        doLookupForFinalRecipient(FINAL_RECIPIENT1, finalRecipient1PartyName, 3, 2, 1, 1);

        //we expect that the DDC lookup time was updated
        final DynamicDiscoveryCertificateEntity finalRecipient1PartyEntitySecondLookup = dynamicDiscoveryCertificateDao.findByCommonName(participantConfigurations.get(FINAL_RECIPIENT1).getPartyName());
        assertNotNull(finalRecipient1PartyEntitySecondLookup);
        assertEquals(PARTY_NAME1, finalRecipient1PartyEntitySecondLookup.getCn());
        final Date dynamicDiscoveryTimeSecondLookup = finalRecipient1PartyEntitySecondLookup.getDynamicDiscoveryTime();
        assertNotNull(dynamicDiscoveryTimeSecondLookup);

        LOG.debug("first lookup [{}], second lookup [{}]", dynamicDiscoveryTimeFirstLookup, dynamicDiscoveryTimeSecondLookup);
        //we expect that the DDC lookup time was updated
        assertTrue(dynamicDiscoveryTimeSecondLookup.compareTo(dynamicDiscoveryTimeFirstLookup) > 0);

        final String finalRecipient2PartyName = DynamicDiscoveryServicePEPPOLConfigurationMockup.participantConfigurations.get(FINAL_RECIPIENT2).getPartyName();
        LOG.info("---first lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT2, finalRecipient2PartyName);
        //FINAL_RECIPIENT1 is associated with PARTY_NAME1 which was already added in the previous lookup, we expect that no new certificates are added in the trusstore and no new parties are added in the Pmode
        //we expect the URL for FINAL_RECIPIENT2 is saved in the database
        doLookupForFinalRecipient(FINAL_RECIPIENT2, finalRecipient2PartyName, 3, 2, 1, 2);

        final String finalRecipient3PartyName = DynamicDiscoveryServicePEPPOLConfigurationMockup.participantConfigurations.get(FINAL_RECIPIENT3).getPartyName();
        LOG.info("---first lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT3, finalRecipient3PartyName);
        //we expect the party and the certificate are added
        //we expect the URL for FINAL_RECIPIENT2 is saved in the database
        doLookupForFinalRecipient(FINAL_RECIPIENT3, finalRecipient3PartyName, 4, 3, 2, 3);
    }

    @Test
    public void dynamicDiscoveryInSMPTriggeredOnce_secondTimeConfigurationIsLoadedFromCache() throws EbMS3Exception {
        //clean up
        cleanBeforeLookup();

        final UserMessage userMessage = buildUserMessage(FINAL_RECIPIENT1);
        //it triggers dynamic discovery lookup in SMP  because toParty is empty
        //id adds in the PMode the discovered party and it adds in the truststore the certificate of the discovered party
        assertNotNull(dynamicDiscoveryPModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, false, ProcessingType.PUSH));

        //we clear the Pmode and the truststore to verify if the dynamic discovery lookup in SMP is triggered again; we can remove the cleanup if we find a better way to verify if dynamic discovery lookup in SMP is triggered
        cleanBeforeLookup();

        //it should take the responder party details from the cache; to verify if dynamic discovery lookup in SMP  is triggered, we check if the responder party is missing from the PMode(we clean the Pmode and truststore above)
        assertNotNull(dynamicDiscoveryPModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, false, ProcessingType.PUSH));

        //verify that the party was not added in the list of available parties in the Pmode(dynamic discovery lookup in SMP  was not triggered again)
        final eu.domibus.common.model.configuration.Configuration configuration = dynamicDiscoveryPModeProvider.getConfiguration();
        final List<Party> pmodePartiesList = configuration.getBusinessProcesses().getParties();
        assertEquals(2, pmodePartiesList.size());
    }

    @Test
    public void c1SubmitsMessageOnServer1_messageSendingFromC2ToC3IsDoneOnServer2() throws EbMS3Exception {
        //clean up
        cleanBeforeLookup();

        final UserMessage userMessage = buildUserMessage(FINAL_RECIPIENT1);
        //it triggers dynamic discovery lookup in SMP  because toParty is empty
        //id adds in the PMode the discovered party and it adds in the truststore the certificate of the discovered party
        assertNotNull(dynamicDiscoveryPModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, false, ProcessingType.PUSH));

        //we simulate the following scenario
        //- message is submitted on server 1 and the dynamic discovery is triggered at submission time: Pmode and truststore are updated normally
        //- message is sent from server 2 via JMS listener; in this case the receiver party is not in the Pmode memory
        //we clean the Pmode memory
        dynamicDiscoveryPModeProvider.refresh();

        //it should trigger a lookup in SMP and the PMode context should be retrieved successfully
        assertNotNull(dynamicDiscoveryPModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, false, ProcessingType.PUSH));
    }

    @Test(expected = AuthenticationException.class)
    public void c1SubmitsMessageToPartyWithExpiredCertificate() throws EbMS3Exception {
        //clean up
        cleanBeforeLookup();

        final UserMessage userMessage = buildUserMessage(FINAL_RECIPIENT4);
        //it triggers dynamic discovery lookup in SMP  because toParty is empty
        //it throws an exception because the discovered certificate is expired
        dynamicDiscoveryPModeProvider.findUserMessageExchangeContext(userMessage, MSHRole.SENDING, false, ProcessingType.PUSH);
    }

    @Test
    public void deleteDDCCertificatesNotDiscoveredInTheLastPeriod() throws EbMS3Exception, KeyStoreException {
        //clean up
        cleanBeforeLookup();

        final String finalRecipient1PartyName = participantConfigurations.get(FINAL_RECIPIENT1).getPartyName();
        LOG.info("---first lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT1, finalRecipient1PartyName);
        //we expect the party and the certificate are added
        doLookupForFinalRecipient(FINAL_RECIPIENT1, finalRecipient1PartyName, 3, 2, 1, 1);

        final String finalRecipient3PartyName = participantConfigurations.get(FINAL_RECIPIENT3).getPartyName();
        LOG.info("---first lookup for final recipient [{}] and party [{}]", FINAL_RECIPIENT3, finalRecipient3PartyName);
        //we expect the party and the certificate are added
        //we expect the URL for FINAL_RECIPIENT2 is saved in the database
        doLookupForFinalRecipient(FINAL_RECIPIENT3, finalRecipient3PartyName, 4, 3, 2, 2);

        //we expect that one entry is added in the database for the final recipient 1
        DynamicDiscoveryCertificateEntity finalRecipient1DdcCertificate = dynamicDiscoveryCertificateDao.findByCommonName(finalRecipient1PartyName);
        assertNotNull(finalRecipient1DdcCertificate);

        //we expect that one entry is added in the database for the final recipient 3
        DynamicDiscoveryCertificateEntity finalRecipient3DdcCertificate = dynamicDiscoveryCertificateDao.findByCommonName(finalRecipient3PartyName);
        assertNotNull(finalRecipient3DdcCertificate);

        //we set the DDC time for the PARTY 1 certificate to 25h ago
        Date ddcTime = DateUtils.addHours(new Date(), 25 * -1);
        finalRecipient1DdcCertificate.setDynamicDiscoveryTime(ddcTime);
        dynamicDiscoveryCertificateDao.createOrUpdate(finalRecipient1DdcCertificate);

        dynamicDiscoveryCertificateService.deleteDDCCertificatesNotDiscoveredInTheLastPeriod(24);

        //we expect that the entry for PARTY_NAME1 was deleted from the database and from the truststore; DDC time is still recent
        DynamicDiscoveryCertificateEntity finalRecipient1DdcCertificateAfterJobRan = dynamicDiscoveryCertificateDao.findByCommonName(finalRecipient1PartyName);
        assertNull(finalRecipient1DdcCertificateAfterJobRan);
        assertNull(multiDomainCertificateProvider.getCertificateFromTruststore(DOMAIN, finalRecipient1PartyName));

        //we expect that the entry for PARTY_NAME2 is still present in the database and in the truststore; DDC time is still recent
        DynamicDiscoveryCertificateEntity finalRecipient3DdcCertificateAfterJobRan = dynamicDiscoveryCertificateDao.findByCommonName(finalRecipient3PartyName);
        assertNotNull(finalRecipient3DdcCertificateAfterJobRan);
        assertNotNull(multiDomainCertificateProvider.getCertificateFromTruststore(DOMAIN, finalRecipient3PartyName));
    }


    private void cleanBeforeLookup() {
        List<String> initialAliasesInTheTruststore = new ArrayList<>();
        initialAliasesInTheTruststore.add("blue_gw");
        initialAliasesInTheTruststore.add("red_gw");
        keepOnlyInitialCertificates(initialAliasesInTheTruststore);
        deleteAllFinalRecipients();
        dynamicDiscoveryPModeProvider.refresh();
    }

    private void doLookupForFinalRecipient(String finalRecipient,
                                           String partyNameAccessPointForFinalRecipient,
                                           int expectedTruststoreEntriesAfterLookup,
                                           int expectedPmodePartiesAfterLookup,
                                           int expectedResponderPartiesAfterLookup,
                                           int expectedFinalRecipientUrlsInDatabase) throws EbMS3Exception {
        final UserMessage userMessage = buildUserMessage(finalRecipient);
        final eu.domibus.common.model.configuration.Configuration initialPModeConfiguration = dynamicDiscoveryPModeProvider.getConfiguration();
        final Collection<Process> pmodeProcesses = initialPModeConfiguration.getBusinessProcesses().getProcesses();

        //do the lookup based on final recipient
        dynamicDiscoveryPModeProvider.lookupAndUpdateConfigurationForToPartyId(getFinalRecipientCacheKey(finalRecipient), userMessage, pmodeProcesses);

        //verify that the party to was added in the UserMessage
        final String toParty = userMessage.getPartyInfo().getToParty();
        assertEquals(partyNameAccessPointForFinalRecipient, toParty);

        //verify that the party certificate was added in the truststore
        List<TrustStoreEntry> trustStoreEntries = multiDomainCertificateProvider.getTrustStoreEntries(DOMAIN);
        assertEquals(expectedTruststoreEntriesAfterLookup, trustStoreEntries.size());

        //verify that the party was added in the list of available parties in the Pmode
        final eu.domibus.common.model.configuration.Configuration configuration = dynamicDiscoveryPModeProvider.getConfiguration();
        final List<Party> pmodePartiesList = configuration.getBusinessProcesses().getParties();
        assertEquals(expectedPmodePartiesAfterLookup, pmodePartiesList.size());
        final Party party1FromPmode = pmodePartiesList.stream().filter(party -> StringUtils.equals(partyNameAccessPointForFinalRecipient, party.getName())).findFirst().orElse(null);
        assertNotNull(party1FromPmode);

        //verify that the party was added in the responder parties for all process
        configuration.getBusinessProcesses().getProcesses().forEach(process -> assertTrue(processContainsResponseParty(process, partyNameAccessPointForFinalRecipient, expectedResponderPartiesAfterLookup)));

        final List<FinalRecipientEntity> allDBfinalRecipientEntities = finalRecipientDao.findAll();
        assertEquals(expectedFinalRecipientUrlsInDatabase, allDBfinalRecipientEntities.size());

        final List<FinalRecipientEntity> finalRecipientEntities = allDBfinalRecipientEntities.stream().filter(finalRecipientEntity -> StringUtils.equals(finalRecipientEntity.getFinalRecipient(), finalRecipient)).collect(Collectors.toList());
        //we expect only 1 final recipient entry in the DB
        assertEquals(1, finalRecipientEntities.size());
        final FinalRecipientEntity finalRecipientEntity = finalRecipientEntities.get(0);
        assertEquals(finalRecipient, finalRecipientEntity.getFinalRecipient());
        assertEquals("http://localhost:9090/" + partyNameAccessPointForFinalRecipient + "/msh", finalRecipientEntity.getEndpointURL());
    }

    private void deleteAllFinalRecipients() {
        //we delete the final recipient entries from the database and we clear the memory cache
        final List<FinalRecipientEntity> allFinalRecipientEntities = finalRecipientDao.findAll();
        finalRecipientDao.deleteAll(allFinalRecipientEntities);
        finalRecipientService.clearFinalRecipientAccessPointUrlsCache();
    }

    private List<TrustStoreEntry> keepOnlyInitialCertificates(List<String> initialAliasesInTheTruststore) {
        List<TrustStoreEntry> trustStoreEntries = multiDomainCertificateProvider.getTrustStoreEntries(DOMAIN);
        //we delete any other certificates apart from blue and red to have a clean state
        final Collection<TrustStoreEntry> certificatesToBeDeleted = trustStoreEntries.stream().filter(trustStoreEntry -> !initialAliasesInTheTruststore.contains(trustStoreEntry.getName())).collect(Collectors.toList());
        certificatesToBeDeleted.stream().forEach(trustStoreEntry -> multiDomainCertificateProvider.removeCertificate(DOMAIN, trustStoreEntry.getName()));

        //check that we have only the initial certificates
        trustStoreEntries = multiDomainCertificateProvider.getTrustStoreEntries(DOMAIN);
        assertEquals(2, trustStoreEntries.size());

        return trustStoreEntries;
    }

    protected boolean processContainsResponseParty(Process process, String partyName, int expectedResponderPartiesAfterLookup) {
        final Set<Party> responderParties = process.getResponderParties();
        assertEquals(expectedResponderPartiesAfterLookup, responderParties.size());
        return responderParties.stream().filter(party -> party.getName().equals(partyName)).findAny().isPresent();
    }

    protected String getFinalRecipientCacheKey(String finalRecipient) {
        return finalRecipient + "cacheKey";
    }

    private UserMessage buildUserMessage(String finalRecipient) {
        UserMessage userMessage = new UserMessage();
        final MpcEntity mpc = new MpcEntity();
        mpc.setValue("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/defaultMPC");
        userMessage.setMpc(mpc);

        Set<MessageProperty> messageProperties = new HashSet<>();
        final MessageProperty finalRecipientProperty = new MessageProperty();
        finalRecipientProperty.setName("finalRecipient");
        finalRecipientProperty.setValue(finalRecipient);
        messageProperties.add(finalRecipientProperty);
        userMessage.setMessageProperties(messageProperties);

        ServiceEntity serviceEntity = new ServiceEntity();
        serviceEntity.setValue("bdx:noprocess");
        serviceEntity.setType("tc1");
        userMessage.setService(serviceEntity);

        final ActionEntity actionEntity = new ActionEntity();
        actionEntity.setValue("TC1Leg1");
        userMessage.setAction(actionEntity);

        final PartyInfo partyInfo = new PartyInfo();
        From from = new From();
        final PartyId fromPartyId = new PartyId();
        fromPartyId.setValue("domibus-blue");
        fromPartyId.setType("urn:oasis:names:tc:ebcore:partyid-type:unregistered");
        final PartyRole partyRole = new PartyRole();
        partyRole.setValue("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        from.setFromRole(partyRole);
        from.setFromPartyId(fromPartyId);
        partyInfo.setFrom(from);


        partyInfo.setTo(new To());

        userMessage.setPartyInfo(partyInfo);
        return userMessage;
    }




}
