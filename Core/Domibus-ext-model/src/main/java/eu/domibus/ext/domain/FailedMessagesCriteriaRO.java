package eu.domibus.ext.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Schema(description = "Search criteria with date + hour (hour is optional)")
public class FailedMessagesCriteriaRO implements Serializable {

    List<String> messageIds;

    @Schema(description = "Date and hour to start the search criteria", type = "string", pattern = "([0-9]{4})-(?:[0-9]{2})-([0-9]{2})(T([0-9]{2})H)?", required = true, example = "2022-01-31T20H")
    private String toDate;

    @Schema(description = "Date and hour to end the search criteria (excluded)", type = "string", pattern = "([0-9]{4})-(?:[0-9]{2})-([0-9]{2})(T([0-9]{2})H)?", required = true, example = "2022-01-31T13H")
    private String fromDate;

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }


    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
}
