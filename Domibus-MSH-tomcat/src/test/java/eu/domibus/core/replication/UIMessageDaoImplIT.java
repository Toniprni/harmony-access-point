package eu.domibus.core.replication;

import eu.domibus.AbstractIT;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageType;
import eu.domibus.api.model.NotificationStatus;
import eu.domibus.common.MessageStatus;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.*;

/**
 * @author Catalin Enache
 * @since 4.1
 */
@Ignore
public class UIMessageDaoImplIT extends AbstractIT {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UIReplicationConfig.class);

    @Autowired
    private UIMessageDaoImpl uiMessageDao;

    private final String messageId1 = UUID.randomUUID().toString();
    private final String messageId2 = UUID.randomUUID().toString();
    private final String messageId3 = UUID.randomUUID().toString();
    private final String conversationId = UUID.randomUUID().toString();
    private final String action1 = "action1";
    private final String serviceValue1 = "serviceValue1";
    private UIMessageEntity uiMessageEntity1, uiMessageEntity2, uiMessageEntity3;


    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Configuration
    static public class UIReplicationConfig {

        @Bean
        public UIMessageDaoImpl uiMessageDao() {
            return new UIMessageDaoImpl();
        }
    }


    @Before
    public void setUp() {
        uiMessageEntity1 = createUIMessageEntity(messageId1, "domibus-blue", "domibus-red", MSHRole.SENDING, action1, serviceValue1);
        uiMessageEntity2 = createUIMessageEntity(messageId2, "domibus-blue", "domibus-red", MSHRole.SENDING, "action2", serviceValue1);
        uiMessageEntity3 = createUIMessageEntity(messageId3, "domibus-red", "domibus-blue", MSHRole.RECEIVING, "action3", "serviceValue2");
        LOG.putMDC(DomibusLogger.MDC_USER, "test_user");
    }


    private UIMessageEntity createUIMessageEntity(final String messageId, String fromId, String toId, MSHRole mshRole, String action, String serviceValue) {

        UIMessageEntity uiMessageEntity = new UIMessageEntity();
        uiMessageEntity.setMessageId(messageId);
        uiMessageEntity.setMessageStatus(MessageStatus.ACKNOWLEDGED);
        uiMessageEntity.setNotificationStatus(NotificationStatus.NOT_REQUIRED);
        uiMessageEntity.setMessageType(MessageType.USER_MESSAGE);
        uiMessageEntity.setTestMessage(false);
        uiMessageEntity.setFromId(fromId);
        uiMessageEntity.setFromScheme("urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1");
        uiMessageEntity.setToId(toId);
        uiMessageEntity.setToScheme("urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4");
        uiMessageEntity.setReceived(new Date());
        uiMessageEntity.setConversationId(conversationId);
        uiMessageEntity.setMshRole(mshRole);
        uiMessageEntity.setSendAttempts(0);
        uiMessageEntity.setSendAttemptsMax(5);
        uiMessageEntity.setAction(action);
        uiMessageEntity.setServiceValue(serviceValue);
        uiMessageEntity.setLastModified(new Date(System.currentTimeMillis()));

        uiMessageDao.create(uiMessageEntity);

        return uiMessageEntity;
    }

    @Test
    public void testFindUIMessageByMessageId() {

        UIMessageEntity uiMessageEntity = uiMessageDao.findUIMessageByMessageId(messageId1);
        Assert.assertNotNull(uiMessageEntity);
        Assert.assertEquals(uiMessageEntity1, uiMessageEntity);

        uiMessageEntity = uiMessageDao.findUIMessageByMessageId(messageId2);
        Assert.assertNotNull(uiMessageEntity);
        Assert.assertEquals(uiMessageEntity2, uiMessageEntity);

        uiMessageEntity = uiMessageDao.findUIMessageByMessageId(messageId3);
        Assert.assertNotNull(uiMessageEntity);
        Assert.assertEquals(uiMessageEntity3, uiMessageEntity);

        Assert.assertNull(uiMessageDao.findUIMessageByMessageId(messageId1 + "123"));
    }

    @Test
    @Ignore("EDELIVERY-8052 Failing tests must be ignored")
    public void testCountMessages() {
        Map<String, Object> filters = new HashMap<>();
        long count;

        filters.put("testMessage", true);
        count = uiMessageDao.countEntries(filters);
        Assert.assertEquals(0, count);

        filters.put("messageSubtype", null);
        filters.put("fromPartyId", "domibus-blue");
        count = uiMessageDao.countEntries(filters);
        Assert.assertEquals(2, count);
    }

    @Test
    public void testFindPaged() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("mshRole", MSHRole.SENDING);
        filters.put("messageType", MessageType.USER_MESSAGE);

        List<UIMessageEntity> uiMessageEntityList = uiMessageDao.findPaged(0, 1, "received", true, filters);
        Assert.assertEquals(1, uiMessageEntityList.size());

        uiMessageEntityList = uiMessageDao.findPaged(1, 1, "received", true, filters);
        Assert.assertEquals(1, uiMessageEntityList.size());

    }

    @Test
    public void testFilterByAction() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("action", action1);
        List<UIMessageEntity> uiMessageEntityList = uiMessageDao.findPaged(0, 10, "received", true, filters);
        Assert.assertEquals(1, uiMessageEntityList.size());
        Assert.assertEquals(action1, uiMessageEntityList.get(0).getAction());

        filters.put("action", "inexistent");
        uiMessageEntityList = uiMessageDao.findPaged(0, 10, "received", true, filters);
        Assert.assertEquals(0, uiMessageEntityList.size());
    }

    @Test
    public void testFilterByService() {
        Map<String, Object> filters = new HashMap<>();
        filters.put("serviceValue", serviceValue1);
        List<UIMessageEntity> uiMessageEntityList = uiMessageDao.findPaged(0, 10, "received", true, filters);
        Assert.assertEquals(2, uiMessageEntityList.size());
        Assert.assertEquals(serviceValue1, uiMessageEntityList.get(0).getServiceValue());
        Assert.assertEquals(serviceValue1, uiMessageEntityList.get(1).getServiceValue());

        filters.put("serviceValue", "inexistent");
        uiMessageEntityList = uiMessageDao.findPaged(0, 10, "received", true, filters);
        Assert.assertEquals(0, uiMessageEntityList.size());
    }

    @Test
    public void testSaveOrUpdate() {

        //save
        final String messageId = UUID.randomUUID().toString();
        UIMessageEntity uiMessageEntity = new UIMessageEntity();
        uiMessageEntity.setMessageId(messageId);
        uiMessageEntity.setMessageStatus(MessageStatus.ACKNOWLEDGED);
        uiMessageEntity.setNotificationStatus(NotificationStatus.NOT_REQUIRED);
        uiMessageEntity.setMessageType(MessageType.USER_MESSAGE);
        uiMessageEntity.setTestMessage(false);
        uiMessageEntity.setConversationId(conversationId);

        uiMessageDao.saveOrUpdate(uiMessageEntity);

        //update
        uiMessageEntity3.setSendAttempts(3);
        uiMessageDao.saveOrUpdate(uiMessageEntity3);
        UIMessageEntity uiMessageByMessageId = uiMessageDao.findUIMessageByMessageId(messageId3);
        Assert.assertEquals(3, uiMessageByMessageId.getSendAttempts());

        Assert.assertNotNull(uiMessageByMessageId.getCreatedBy());
        Assert.assertNotNull(uiMessageByMessageId.getCreationTime());
        Assert.assertNotNull(uiMessageByMessageId.getModifiedBy());
        Assert.assertNotNull(uiMessageByMessageId.getModificationTime());

        Assert.assertNotEquals(uiMessageByMessageId.getCreationTime(), uiMessageByMessageId.getModificationTime());
    }

}