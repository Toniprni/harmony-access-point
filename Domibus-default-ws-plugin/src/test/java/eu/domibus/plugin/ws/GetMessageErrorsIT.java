
package eu.domibus.plugin.ws;

import eu.domibus.api.model.MSHRole;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.error.ErrorLogDao;
import eu.domibus.core.error.ErrorLogEntry;
import eu.domibus.core.message.dictionary.MshRoleDao;
import eu.domibus.plugin.ws.generated.body.ErrorResultImplArray;
import eu.domibus.plugin.ws.generated.body.GetErrorsRequest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


/**
 * This JUNIT implements the Test cases Get Message Errors-01 and Get Message Errors-02.
 *
 * @author martifp
 */
public class GetMessageErrorsIT extends AbstractBackendWSIT {

    @Autowired
    ErrorLogDao errorLogDao;

    @Autowired
    MshRoleDao mshRoleDao;

    /**
     * Tests that the list of errors is not empty for a certain message.
     */
    @Test
    public void testGetMessageErrorsOk() {
        String messageId = "9008713e-1912-460c-97b3-40ec12a29f49@domibus.eu";
        ErrorLogEntry logEntry = new ErrorLogEntry();
        logEntry.setMessageInErrorId(messageId);
        logEntry.setMshRole(mshRoleDao.findOrCreate(MSHRole.RECEIVING));
        logEntry.setErrorCode(ErrorCode.EBMS_0004);
        logEntry.setTimestamp(new Date());
        errorLogDao.create(logEntry);

        GetErrorsRequest errorsRequest = createMessageErrorsRequest(messageId);
        ErrorResultImplArray response = webServicePluginInterface.getMessageErrors(errorsRequest);
        Assert.assertFalse(response.getItem().isEmpty());
    }

    /**
     * Tests that the list of errors is empty for a certain message since there were no errors in the transaction.
     */
    @Test
    public void testGetEmptyMessageErrorsList() {

        String messageId = "2809cef6-240f-4792-bec1-7cb300a34679@domibus.eu";
        GetErrorsRequest errorsRequest = createMessageErrorsRequest(messageId);
        ErrorResultImplArray response = webServicePluginInterface.getMessageErrors(errorsRequest);
        Assert.assertTrue(response.getItem().isEmpty());
    }

    private GetErrorsRequest createMessageErrorsRequest(final String messageId) {

        GetErrorsRequest errorsRequest = new GetErrorsRequest();
        errorsRequest.setMessageID(messageId);
        return errorsRequest;
    }
}
