package domibus.ui.functional;

import ddsl.dcomponents.DomibusPage;
import ddsl.enums.DRoles;
import ddsl.enums.PAGES;
import pages.jms.JMSMonitoringPage;
import utils.BaseTest;
import org.json.JSONObject;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.Audit.AuditPage;
import pages.login.LoginPage;
import pages.msgFilter.MessageFilterModal;
import pages.msgFilter.MessageFilterPage;
import pages.pmode.current.PModeArchivePage;
import pages.pmode.current.PModeCofirmationModal;
import pages.pmode.current.PModeCurrentPage;
import pages.pmode.parties.PModePartiesPage;
import pages.pmode.parties.PartyModal;
import utils.Generator;
import utils.TestUtils;

import java.util.HashMap;
import java.util.List;


public class AuditPgTest extends BaseTest {

    JSONObject descriptorObj = TestUtils.getPageDescriptorObject(PAGES.AUDIT);

    /*    AU-6 - Filter events so that there are no results   */
    @Test(description = "AU-6", groups = {"multiTenancy", "singleTenancy"})
    public void searchWithNoData() throws Exception {
        SoftAssert soft = new SoftAssert();

        log.info("Generate Random string for Username");
        String user = Generator.randomAlphaNumeric(10);
        log.info("Create user with rest service");
        rest.createUser(user, DRoles.ADMIN, data.defaultPass(), null);

        log.info("Login with admin user");
        log.info("Navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        ;

        AuditPage page = new AuditPage(driver);
        page.getFilters().setFilterData("table", "User");
        log.info("Select logged in user username in User input filter");
        page.getFilters().setFilterData("user", user);
        log.info("Click on Search button");
        page.getFilters().getSearchButton().click();
        log.info("Search result count:" + page.getFilters().getPagination().getTotalItems());
        log.info("Validate no data presence for this user on audit page");
        soft.assertTrue(page.getFilters().getPagination().getTotalItems() == 0, "Search has no data");
        soft.assertAll();
    }

    /*   AU-7 - Delete all criteria and press Search    */
    @Test(description = "AU-7", groups = {"multiTenancy", "singleTenancy"})
    public void deleteSearchCriteria() throws Exception {
        SoftAssert soft = new SoftAssert();

        String user = Generator.randomAlphaNumeric(10);
        log.info("Create user with rest service");
        rest.createUser(user, DRoles.ADMIN, data.defaultPass(), null);

        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);

        AuditPage page = new AuditPage(driver);
        page.grid().waitForRowsToLoad();
        int prevCount = page.grid().getPagination().getTotalItems();
        log.info("started out with items " + prevCount);

        log.info("Set Table filter data as User");
        page.filters().getTableFilter().selectOptionByText("User");

        log.info("Set User filter data as created user");
        page.filters().getUserFilter().selectOptionByText(user);

        log.info("Click on search button");
        page.getFilters().getSearchButton().click();
        page.grid().waitForRowsToLoad();

        log.info("Total search record is :" + page.grid().getPagination().getTotalItems());
        page.refreshPage();
        page.wait.forElementToBeVisible(page.auditPageHeader);
        page.grid().waitForRowsToLoad();

        soft.assertTrue(page.grid().getPagination().getTotalItems() == prevCount, "Page shows all records after deletion of all selected filter values");
        soft.assertAll();
    }


    /*   AU-14 - Check Action On Audit page Grid data for Record created on Download action on Message page  */
    @Test(description = "AU-14", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
    public void messageDownloadedLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser());
        DomibusPage page = new DomibusPage(driver);
        String user = Generator.randomAlphaNumeric(10);
        log.info("Create Plugin user with rest service");
        rest.createPluginUser(user, DRoles.ADMIN, data.defaultPass(), null);
        log.info("Upload pmode");
        rest.uploadPMode("pmodes/pmode-blue.xml", null);
        log.info("Send message");
        String messID = messageSender.sendMessage(user, data.defaultPass(), null, null);
        log.info("Download message");
        rest.downloadMessage(messID, null);
        log.info("Navigate to Audit page");
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);
        log.info("Set Table data as Message");
        auditPage.filters().getTableFilter().selectOptionByText("Message");
        log.info("Select Created as Action Field data");
        auditPage.filters().getActionFilter().selectOptionByText("Downloaded");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Deleted");
        boolean result = auditPage.grid().getRowInfo(0).containsValue("Message")
                && auditPage.grid().getRowInfo(0).containsValue("Downloaded")
                && auditPage.grid().getRowInfo(0).containsValue(messID);
        soft.assertTrue(result, "Top row has Table value as Message, User value as Admin & Action as Downloaded ");
        soft.assertAll();
    }


    /*  AU-15 - Check action on Create event on Message filter  */
    @Test(description = "AU-15", groups = {"multiTenancy", "singleTenancy"})
    public void msgFilterCreation() throws Exception {
        String actionName = Generator.randomAlphaNumeric(5);
        log.info("Create one message filter with action field value as :" + actionName);
        rest.createMessageFilter(actionName, null);

        SoftAssert soft = new SoftAssert();
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);

        AuditPage auditPage = new AuditPage(driver);

        log.info("Set table filter as Message filter");
        auditPage.filters().getTableFilter().selectOptionByText("Message filter");

        log.info("Click on search button");
        auditPage.filters().clickSearch();
        auditPage.grid().waitForRowsToLoad();

        log.info("Validate log presence on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Created"), "Message filter action is logged successfully");
        soft.assertAll();

    }

    /*  AU-16 - Check action on edit  event on Message filter   */
    @Test(description = "AU-16", groups = {"multiTenancy", "singleTenancy"})
    public void msgFilterEdit() throws Exception {
        SoftAssert soft = new SoftAssert();
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.MESSAGE_FILTER);
        DomibusPage page = new DomibusPage(driver);
        MessageFilterPage mPage = new MessageFilterPage(driver);
        String actionName = Generator.randomAlphaNumeric(5);
        if (mPage.grid().getPagination().getTotalItems() > 0) {
            log.info("Select row with index 0 if total grid count >0");
            mPage.grid().selectRow(0);
            log.info("Click on edit button");
            mPage.getEditBtn().click();
            MessageFilterModal modal = new MessageFilterModal(driver);
            log.info("Update action field value");
            modal.getActionInput().fill(actionName);
            log.info("Click on ok ");
            modal.clickOK();
            log.info("saving changes");
            mPage.saveAndConfirmChanges();
        }
        log.info("Navigate to Audit page");
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);
        soft.assertEquals(auditPage.getTitle(), descriptorObj.getString("title"), "page is loaded successfully");
        log.info("Set all data in search filters");
        auditPage.getFilters().setFilterData("table", "Message filter");
        log.info("Click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Modified"), "Correct action is logged");
        soft.assertAll();

    }

    /* AU-17 - Check action on Move up/Move Down on Message Filter */
    @Test(description = "AU-17", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
    public void msgFilterMoveAction() throws Exception {
        SoftAssert soft = new SoftAssert();
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.MESSAGE_FILTER);
        DomibusPage page = new DomibusPage(driver);
        MessageFilterPage mPage = new MessageFilterPage(driver);
        if (mPage.grid().getPagination().getTotalItems() == 1) {
            String actionName = Generator.randomAlphaNumeric(5);
            log.info("Create message filter");
            rest.createMessageFilter(actionName, null);
        }
        log.info("Select last row");
        mPage.grid().selectRow(mPage.grid().getPagination().getTotalItems() - 1);
        log.info("Select last row");
        mPage.getMoveUpBtn().click();
        log.info("Click on save button then yes from confirmation pop up");
        mPage.saveAndConfirmChanges();
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);
        soft.assertEquals(auditPage.getTitle(), descriptorObj.getString("title"), "page is loaded successfully");
        log.info("Select data in search filters");
        auditPage.getFilters().setFilterData("table", "Message filter");
        log.info("Click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        log.info("Validate logs on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Modified"), "Correct action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).containsValue("Modified"), "Correct action is logged");
        soft.assertAll();


    }

    /*  AU-18 - Check action on Delete on Message Filter    */
    @Test(description = "AU-18", groups = {"multiTenancy", "singleTenancy"})
    public void msgFilterDeletion() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application and navigate to Message filter page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.MESSAGE_FILTER);
        DomibusPage page = new DomibusPage(driver);
        String actionName = Generator.randomAlphaNumeric(5);
        log.info("Create one message filter");
        rest.createMessageFilter(actionName, null);
        MessageFilterPage mPage = new MessageFilterPage(driver);
        log.info("Select last row");
        mPage.grid().selectRow(mPage.grid().getPagination().getTotalItems() - 1);
        log.info("Click on delete button");
        mPage.getDeleteBtn().click();
        log.info("Click save button");
        mPage.getSaveBtn().click();
        log.info("Click save button then yes button on confirmation pop up");
        mPage.getConfirmation().confirm();
        log.info("Success message shown :" + page.getAlertArea().getAlertMessage());
        log.info("Navigate to Audit page");
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);
        soft.assertEquals(auditPage.getTitle(), descriptorObj.getString("title"), "page is loaded successfully");
        log.info("Set all search filter");
        auditPage.getFilters().setFilterData("table", "Message filter");
        log.info("Click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        log.info("Validate presence of log on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Deleted"), "Correct action is logged");
        soft.assertAll();
    }


    /*  AU-19 - Check action on Text update on Pmode -current page  */
    @Test(description = "AU-19", groups = {"multiTenancy", "singleTenancy"})
    public void txtUpdatePmode() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Pmode current page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_CURRENT);
        log.info("Upload pmode");
        rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
        PModeCurrentPage pPage = new PModeCurrentPage(driver);
        log.info("Extract data from current Pmode");
        String beforeEditPmode = pPage.getTextArea().getText();
        log.info("Modify some text");
        String afterEditPmode = beforeEditPmode.replaceAll("\\t", " ").replaceAll("localhost", "mockhost");
        log.info("Fill pmode current area with updated pmode text");
        pPage.getTextArea().fill(afterEditPmode);
        log.info("Click on save button");
        pPage.getSaveBtn().click();
        PModeCofirmationModal modal = new PModeCofirmationModal(driver);
        log.info("Enter description");
        modal.getDescriptionTextArea().fill("Valid Modification");
        log.info("Click on ok button");
        modal.clickOK();
        DomibusPage page = new DomibusPage(driver);
        AuditPage auditPage = new AuditPage(driver);
        log.info("Navigate to Audit page");
        page.getSidebar().goToPage(PAGES.AUDIT);
        log.info("Select Pmode as Table field data");
        auditPage.getFilters().setFilterData("table", "Pmode");
        log.info("click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        log.info("Validate data on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).get("Action") != null, "Proper action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).get("Action") != null, "Proper action is logged");
        soft.assertAll();
    }

    /*  AU-20 - Check action on Successful Upload on Pmode-current  */
    @Test(description = "AU-20", groups = {"multiTenancy", "singleTenancy"})
    public void pmodeUpload() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Upload pmode");
        rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Select Pmode as Table field data");
        AuditPage page = new AuditPage(driver);
        page.getFilters().setFilterData("table", "Pmode");
        log.info("click on search button");
        page.getFilters().getSearchButton().click();
        page.grid().waitForRowsToLoad();
        log.info("Validate data on Audit page");
        soft.assertTrue(page.grid().getRowInfo(0).get("Action") != null, "Proper action is logged");
        soft.assertTrue(page.grid().getRowInfo(1).get("Action") != null, "Proper action is logged");
        soft.assertAll();
    }

    /*  AU-22 - Login as domain admin, go to page Parties and Create parties    */
    @Test(description = "AU-22", groups = {"multiTenancy", "singleTenancy"})
    public void createParty() throws Exception {
        log.info("Upload pmode");
        rest.uploadPMode("pmodes/multipleParties.xml", null);
        String newPartyName = Generator.randomAlphaNumeric(5);
        SoftAssert soft = new SoftAssert();

        log.info("login into application and navigate to Pmode parties page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_PARTIES);
        PModePartiesPage pPage = new PModePartiesPage(driver);

        log.info("Validate new button is enabled");
        soft.assertTrue(pPage.getNewButton().isEnabled(), "New button is enabled");

        log.info("Click on New button");
        pPage.getNewButton().click();
        PartyModal modal = new PartyModal(driver);
        log.info("Fill new party info");
        modal.fillNewPartyForm(newPartyName, "http://test.com", "pid");

        log.info("Click ok button");
        modal.clickOK();


        pPage.getSaveButton().click();
        log.info("validate presence of success message");
        soft.assertTrue(!pPage.getAlertArea().isError(), "page shows success message");

        log.info("Go to Audit page");
        pPage.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Set all search filter data");
        auditPage.filters().getTableFilter().selectOptionByText("Pmode");

        log.info("Click in search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();

        log.info("Validate data on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).get("Action") != null, "Proper action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).get("Action") != null, "Proper action is logged");

        soft.assertAll();
    }

    /*  AU-23 - Login as domain admin, go to page Parties and Edit parties  */
    @Test(description = "AU-23", groups = {"multiTenancy", "singleTenancy"})
    public void editParty() throws Exception {
        log.info("upload pmode");
        rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
        String newPartyName = Generator.randomAlphaNumeric(5);
        SoftAssert soft = new SoftAssert();
        log.info("Login and navigate to pmode parties page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_PARTIES);
        PModePartiesPage pPage = new PModePartiesPage(driver);
        log.info("select row 0");
        pPage.grid().selectRow(0);
        log.info("Click edit button");
        pPage.getEditButton().click();
        PartyModal modal = new PartyModal(driver);
        log.info("Fill new party info");
        modal.getNameInput().fill(newPartyName);
        log.info("Fill endpoint value");
        modal.getEndpointInput().fill("http://" + newPartyName.toLowerCase() + ".com");
        log.info("Click ok button");
        modal.clickOK();

        log.info("Click SAVE button");
        pPage.getSaveButton().click();

        log.info("waiting for confirmation");
        pPage.wait.forXMillis(5000);
        pPage.wait.forElementToBeVisible(pPage.getAlertArea().alertMessage);


        log.info("Validate presence of success message");
        DomibusPage page = new DomibusPage(driver);
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);

        auditPage.grid().waitForRowsToLoad();

        log.info("Set all search filter data");
        auditPage.getFilters().setFilterData("table", "Pmode");
        log.info("Click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        log.info("Validate data on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).get("Action") != null, "Proper action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).get("Action") != null, "Proper action is logged");
        soft.assertAll();
    }

    /*  AU-24 - Login as domain admin, go to page Parties and Delete parties    */
    @Test(description = "AU-24", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
    public void deleteParty() throws Exception {
        log.info("upload pmode");
        rest.uploadPMode("pmodes/multipleParties.xml", null);
        SoftAssert soft = new SoftAssert();

        log.info("Login and navigate to pmode parties page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_PARTIES);
        PModePartiesPage pPage = new PModePartiesPage(driver);
        pPage.grid().scrollToAndSelect("Party Name", "red_gw");
        pPage.getDeleteButton().click();
        pPage.getSaveButton().click();
        log.info("Message shown : " + pPage.getAlertArea().getAlertMessage());

        pPage.getSidebar().goToPage(PAGES.AUDIT);

        AuditPage auditPage = new AuditPage(driver);

        log.info("Set all search filter data");
        auditPage.filters().getTableFilter().selectOptionByText("Pmode");
        auditPage.filters().clickSearch();
        auditPage.grid().waitForRowsToLoad();

        log.info("Validate data on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Created"), "Created action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).containsValue("Deleted"), "Deleted action is logged");

        soft.assertAll();
    }

    /*   AU-25 - Login as domain admin, go to page PMode Archive and Download old/current  PModes   */
    @Test(description = "AU-25", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
    public void pmodeDownload() throws Exception {
        rest.uploadPMode("pmodes/doNothingInvalidRed.xml", null);

        SoftAssert soft = new SoftAssert();
        log.info("getting pmode id");
        Integer pmodeID = rest.getLatestPModeID(null);
        log.info("downloading PMODE with id " + pmodeID);
        String filename = rest.downloadPmode(null, pmodeID);
        log.info("downloaded file with name" + filename);

        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        AuditPage page = new AuditPage(driver);

        page.getFilters().setFilterData("table", "Pmode");
        log.info("click on search button");
        page.getFilters().getSearchButton().click();
        page.grid().waitForRowsToLoad();
        log.info("Validate data on Audit page");

        HashMap<String, String> info = page.grid().getRowInfo(0);

        soft.assertEquals(info.get("Table"), "Pmode", "Table column has value Pmode");
        soft.assertEquals(info.get("Action"), "Downloaded", "Action column has value Downloaded");
        soft.assertEquals(info.get("Id"), String.valueOf(pmodeID), "Correct pmodeID listed");

        soft.assertAll();

    }

    /*  AU-26 - Login as domain admin, go to page PMode Archive and Restore  old  PModes    */
    @Test(description = "AU-26", groups = {"multiTenancy", "singleTenancy"})
    public void restorePmodeFromArchive() throws Exception {
        log.info("upload pmode");
        rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
        SoftAssert soft = new SoftAssert();
        log.info("Login and navigate to pmode parties page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_ARCHIVE);
        PModeArchivePage pAuditPage = new PModeArchivePage(driver);
        PModePartiesPage pPage = new PModePartiesPage(driver);
        DomibusPage page = new DomibusPage(driver);
        if (pAuditPage.grid().getRowsNo() == 1) {
            log.info("Upload pmode if grid row count is 1");
            rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
        }
        log.info("Select row with index 1");
        pAuditPage.grid().selectRow(1);
        log.info("Click on restore button");
        pAuditPage.getRestoreButton().click();
        log.info("Click on save and then yes button on confirmation pop up");
        pAuditPage.getConfirmation().confirm();
        log.info("Success message shown : " + pPage.getAlertArea().getAlertMessage());
        page.getSidebar().goToPage(PAGES.AUDIT);
        AuditPage auditPage = new AuditPage(driver);
        auditPage.waitForTitle();
        auditPage.grid().waitForRowsToLoad();
        log.info("Set all search filters");
        auditPage.getFilters().setFilterData("table", "Pmode");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();
        log.info("click on search button");
        log.info("Validate log presence on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).get("Action") != null, "Proper action is logged");
        soft.assertTrue(auditPage.grid().getRowInfo(1).get("Action") != null, "Proper action is logged");
        soft.assertAll();
    }

    /*  AU-27 - Login as domain admin, go to page PMode Archive and Delete old PModes   */
    @Test(description = "AU-27", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
    public void deletePmodeFromArchive() throws Exception {
        log.info("upload pmode");
        rest.uploadPMode("pmodes/Edelivery-blue.xml", null);

        SoftAssert soft = new SoftAssert();

        log.info("Login and navigate to pmode archive page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.PMODE_ARCHIVE);

        PModeArchivePage archivePage = new PModeArchivePage(driver);


        if (archivePage.grid().getRowsNo() == 1) {
            log.info("Upload pmode");
            rest.uploadPMode("pmodes/Edelivery-blue.xml", null);
            archivePage.refreshPage();
            archivePage.grid().waitForRowsToLoad();
        }

        log.info("Select row with index 1");
        archivePage.grid().selectRow(1);

        log.info("Click on delete button");
        archivePage.getDeleteButton().click();
        log.info("click on save button");
        archivePage.getSaveButton().click();
        log.info("Click on yes button on confirmation pop up");
        archivePage.getConfirmation().confirm();

        log.info("Success message shown : " + archivePage.getAlertArea().getAlertMessage());
        archivePage.getSidebar().goToPage(PAGES.AUDIT);

        AuditPage auditPage = new AuditPage(driver);
        log.info("Set all search filters");
        auditPage.getFilters().setFilterData("table", "Pmode Archive");

        log.info("Click on search button");
        auditPage.getFilters().getSearchButton().click();
        auditPage.grid().waitForRowsToLoad();

        log.info("Validate data on Audit page");
        soft.assertTrue(auditPage.grid().getRowInfo(0).containsValue("Deleted"), "Delete action is logged");

        soft.assertAll();
    }

    /*    AU-28 - Check action on New user creation     */
    @Test(description = "AU-28", groups = {"multiTenancy", "singleTenancy"})
    public void createUserLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Create user with rest call");
        String username = Generator.randomAlphaNumeric(10);
        rest.createUser(username, DRoles.ADMIN, data.defaultPass(), null);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Select User in Table input filter");
        auditPage.getFilters().setFilterData("table", "User");
        log.info("Select Created as Action in filter");
        auditPage.getFilters().setFilterData("Action", "Created");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Created");
        Boolean result1 = auditPage.grid().getRowInfo(0).containsValue("Created");
        Boolean result = auditPage.grid().getRowInfo(0).containsValue("User");

        soft.assertTrue(result, "Top row shows Action as created ");
        soft.assertTrue(result1, "Top row has Table value as User");
        soft.assertAll();
    }

    /*  AU-29 - Check action on Edit user event */
    @Test(description = "AU-29", groups = {"multiTenancy", "singleTenancy"})
    public void editUserLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Create user with rest call");
        String username = Generator.randomAlphaNumeric(10);
        rest.createUser(username, DRoles.ADMIN, data.defaultPass(), null);
        HashMap<String, String> params = new HashMap<>();
        params.put("password", data.getNewTestPass());
        rest.updateUser(username, params, null);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Select User in Table input filter");
        auditPage.getFilters().setFilterData("table", "User");
        log.info("Select Created as Action in filter");
        auditPage.getFilters().setFilterData("Action", "Modified");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Modified");
        boolean result = auditPage.grid().getRowInfo(0).containsValue("User")
                && auditPage.grid().getRowInfo(0).containsValue("Modified");
        soft.assertTrue(result, "Top row has Table value as User, User value as Admin & Action as Modified ");
        soft.assertAll();
    }


    /*  AU-30 - Check action on Delete user event   */
    @Test(description = "AU-30", groups = {"multiTenancy", "singleTenancy"})
    public void deleteUserLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Create user with rest call");
        String username = Generator.randomAlphaNumeric(10);
        rest.createUser(username, DRoles.ADMIN, data.defaultPass(), null);
        rest.deleteUser(username, null);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Select User in Table input filter");
        auditPage.getFilters().setFilterData("table", "User");
        log.info("Select Created as Action in filter");
        auditPage.getFilters().setFilterData("Action", "Modified");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Modified");
        boolean result = auditPage.grid().getRowInfo(0).containsValue("User")
                && auditPage.grid().getRowInfo(0).containsValue("Modified");
        soft.assertTrue(result, "Top row has Table value as User, User value as Admin & Action as Modified ");
        soft.assertAll();
    }


    /*    AU-39 - Check action on plugin user creation event    */
    @Test(description = "AU-39", groups = {"multiTenancy", "singleTenancy"})
    public void createPluginUserLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Create user with rest call");
        String username = Generator.randomAlphaNumeric(10);
        rest.createPluginUser(username, DRoles.ADMIN, data.defaultPass(), null);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Select PluginUser as Table field data");
        auditPage.getFilters().setFilterData("table", "PluginUser");
        log.info("Select Created as Action Field data");
        auditPage.getFilters().setFilterData("Action", "Created");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Created");
        boolean result = auditPage.grid().getRowInfo(0).containsValue("PluginUser")
                && auditPage.grid().getRowInfo(0).containsValue("Created");
        soft.assertTrue(result, "Top row has Table value as PluginUser, User value as Admin & Action as created ");
        soft.assertAll();
    }


    /*   AU-40 - Check action on plugin user Delete event   */
    @Test(description = "AU-40", groups = {"multiTenancy", "singleTenancy"})
    public void deletePluginUserLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        log.info("Create user with rest call");
        String username = Generator.randomAlphaNumeric(10);
        rest.createPluginUser(username, DRoles.ADMIN, data.defaultPass(), null);
        rest.deletePluginUser(username, null);
        AuditPage auditPage = new AuditPage(driver);

        log.info("Select PluginUser as Table field data");
        auditPage.getFilters().setFilterData("table", "PluginUser");
        log.info("Select Created as Action Field data");
        auditPage.getFilters().setFilterData("Action", "Deleted");
        auditPage.getFilters().getSearchButton().click();
        log.info("Validate non zero Search result count ");
        soft.assertTrue(auditPage.getFilters().getPagination().getTotalItems() > 0, "Search has records");
        log.info("Validate top record Action as Deleted");
        boolean result = auditPage.grid().getRowInfo(0).containsValue("PluginUser")
                && auditPage.grid().getRowInfo(0).containsValue("Deleted");
        soft.assertTrue(result, "Top row has Table value as PluginUser, User value as Admin & Action as Deleted ");
        soft.assertAll();
    }

    /* AU-8 Verify data after changing domain*/
    @Test(description = "AU-8", groups = {"multiTenancy"})
    public void changeDomain() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);

        AuditPage aPage = new AuditPage(driver);

        log.info("Check total number of records for default domain");
        int defaultDomainGridCount = aPage.grid().getPagination().getTotalItems();

        log.info("Extract all row info ");
        List<HashMap<String, String>> defaultRowInfos = aPage.grid().getAllRowInfo();

        log.info("Change domain");
        aPage.getDomainSelector().selectOptionByIndex(1);

        log.info("Extract total number of items for second domain");
        int secondGridCount = aPage.grid().getPagination().getTotalItems();

        log.info("Extract all row infos");
        List<HashMap<String, String>> secondRowInfos = aPage.grid().getAllRowInfo();

        log.info("Verify grid row data for both domains");
        if (defaultDomainGridCount == 0 && secondGridCount == 0) {
            log.info("Both domains have no data on this page");
        } else if (defaultDomainGridCount != secondGridCount) {
            log.info("Both domains have different number of data");
        } else if (defaultRowInfos == secondRowInfos) {
            log.info("Both domains have same number of data but all are different");
            soft.assertFalse(defaultRowInfos == secondRowInfos, "compare both domains grid row data");
        } else {
            log.info("Something went wrong on this page");
        }
        soft.assertAll();

    }

    /* Verify data and page number after changing domain from second page of default domain*/
    @Test(description = "AU-10", groups = {"multiTenancy"})
    public void changeDomainFromSecPage() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.AUDIT);
        AuditPage aPage = new AuditPage(driver);

        log.info("Extract total number of items for default domain");
        int defaultGridRowCount = aPage.grid().getPagination().getTotalItems();

        log.info("Check if pagination is present");
        if (!aPage.grid().getPagination().isPaginationPresent()) {
            log.info("Default domain grid has data less than 10 so no pagination exists");
        } else {
            log.info("Navigate to page 2");
            aPage.grid().getPagination().goToPage(2);
            soft.assertTrue(aPage.grid().getPagination().getActivePage() == 2, "User is on second page of Default domain");

            log.info("Change domain");
            aPage.getDomainSelector().selectOptionByIndex(1);
            log.info("Extract total number of items for second domain");
            int secondGridRowCount = aPage.grid().getPagination().getTotalItems();

            log.info("Check if pagination is present");
            if (aPage.grid().getPagination().isPaginationPresent()) {
                log.info("Pagination is present for second domain");
                if (aPage.grid().getPagination().getActivePage() == 1) {
                    log.info("Active page is " + aPage.grid().getPagination().getActivePage());
                }
            }
            log.info("Check if both domains have different number of data");
            soft.assertTrue(defaultGridRowCount != secondGridRowCount, " Both domains have different number of records");
        }
        soft.assertAll();
    }

    /* AU-12 Check log presence on jms message deletion event*/
    @Test(description = "AU-12", groups = {"multiTenancy", "singleTenancy"})
    public void JmsMsgDeletionLog() throws Exception {
        SoftAssert soft = new SoftAssert();
        log.info("Login into application with Admin credentials and navigate to Audit page");
        login(data.getAdminUser()).getSidebar().goToPage(PAGES.JMS_MONITORING);
        JMSMonitoringPage jmsPage = new JMSMonitoringPage(driver);

        do {
            log.info("Check total number of records");
            if (jmsPage.grid().getPagination().getTotalItems() > 0) {
                log.info("Wait for grid row to load");
                jmsPage.grid().waitForRowsToLoad();

                log.info("Select first row");
                jmsPage.grid().selectRow(0);

            } else {
                log.info("Select jms queue with some messages ");
                jmsPage.filters().getJmsQueueSelect().selectQueueWithMessagesNotDLQ();

                log.info("Wait for grid row to load");
                jmsPage.grid().waitForRowsToLoad();

                log.info("Select first row");
                jmsPage.grid().selectRow(0);
            }

            log.info("Extract first row data");
            HashMap<String, String> firstRowData = jmsPage.grid().getRowInfo(0);

            log.info("Extract value for ID field for first row");
            String jmsMsgId = firstRowData.get("ID");

            log.info("Click on delete button");
            jmsPage.getDeleteButton().click();

            log.info("Click on save button");
            jmsPage.getSaveButton().click();

            AuditPage aPage = new AuditPage(driver);
            log.info("Navigate to Audit page");
            jmsPage.getSidebar().goToPage(PAGES.AUDIT);

            log.info("Verify first row Action column data as Deleted");
            soft.assertTrue(aPage.grid().getRowInfo(0).containsValue("Deleted"));

            log.info("Verify first row Table column data as Jms Message");
            soft.assertTrue(aPage.grid().getRowInfo(0).containsValue("Jms message"));

            log.info("Verify first row ID column data as ID shown for Message on Jms monitoring page");
            soft.assertTrue(aPage.grid().getRowInfo(0).containsValue(jmsMsgId));

            log.info("Break from loop if domain name is domain1 for multitenancy or null for single tenancy");
            if (aPage.getDomainFromTitle() == null || aPage.getDomainFromTitle().equals("domain1")) {
                break;
            }


            if (data.isIsMultiDomain()) {
                log.info("Navigate to jms monitoring page in case of multitenancy");
                aPage.getSidebar().goToPage(PAGES.JMS_MONITORING);

                log.info("Change Domain");
                jmsPage.getDomainSelector().selectOptionByIndex(1);

                log.info("Wait for grid row to load");
                jmsPage.grid().waitForRowsToLoad();

            }
        } while (jmsPage.getDomainFromTitle() == null || jmsPage.getDomainFromTitle().equals("domain1"));
        soft.assertAll();


    }
}
