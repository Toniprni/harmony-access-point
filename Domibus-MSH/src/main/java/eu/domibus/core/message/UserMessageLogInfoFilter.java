package eu.domibus.core.message;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@Service(value = "userMessageLogInfoFilter")
public class UserMessageLogInfoFilter extends MessageLogInfoFilter {

    @Override
    public String filterMessageLogQuery(String column, boolean asc, Map<String, Object> filters) {
        String query = "select new eu.domibus.core.message.MessageLogInfo(" +
                "log.messageId," +
                "log.messageStatus," +
                "log.notificationStatus," +
                "log.mshRole," +
                "log.messageType," +
                "log.deleted," +
                "log.received," +
                "log.sendAttempts," +
                "log.sendAttemptsMax," +
                "log.nextAttempt," +
                "message.collaborationInfo.conversationId," +
                "partyFrom.value," +
                "partyTo.value," +
                (isFourCornerModel() ? "propsFrom.value," : "'',") +
                (isFourCornerModel() ? "propsTo.value," : "'',") +
                "info.refToMessageId," +
                "log.failed," +
                "log.restored," +
                "message.messageSubtype," +
                "log.messageFragment," +
                "log.sourceMessage," +
                MESSAGE_COLLABORATION_INFO_ACTION + "," +
                MESSAGE_COLLABORATION_INFO_SERVICE_TYPE + "," +
                MESSAGE_COLLABORATION_INFO_SERVICE_VALUE +
                ")" + getQueryBody();
        StringBuilder result = filterQuery(query, column, asc, filters);
        return result.toString();
    }

    public String countUserMessageLogQuery(boolean asc, Map<String, Object> filters) {
        String query = "select count(message.id)" + getQueryBody();

        StringBuilder result = filterQuery(query, null, asc, filters);
        return result.toString();
    }

    /**
     * Constructs the query body based on different conditions
     *
     * @return String query body
     */
    private String getQueryBody() {
        return
                " from UserMessageLog log " +
                        "join log.userMessage message " +
                        (isFourCornerModel() ?
                                "left join message.messageProperties propsFrom "  +
                                "left join message.messageProperties propsTo " : StringUtils.EMPTY) +
                        "left join message.partyInfo.from.partyId partyFrom " +
                        "left join message.partyInfo.to.partyId partyTo " +
                        (isFourCornerModel() ?
                                "where propsFrom.name = 'originalSender' "  +
                                "and propsTo.name = 'finalRecipient' " : StringUtils.EMPTY);

    }



}
