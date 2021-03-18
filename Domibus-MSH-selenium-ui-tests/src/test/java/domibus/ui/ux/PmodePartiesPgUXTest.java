package domibus.ui.ux;


import ddsl.enums.PAGES;
import domibus.ui.SeleniumTest;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.pmode.parties.PModePartiesPage;

import java.util.HashMap;
import java.util.List;

public class PmodePartiesPgUXTest extends SeleniumTest {
	
	@Test(description = "PMP-1", groups = {"multiTenancy", "singleTenancy"})
	public void openPModePartiesPage() throws Exception {
		
		rest.pmode().uploadPMode("pmodes/doNothingInvalidRed.xml", null);
		
		SoftAssert soft = new SoftAssert();
		
		PModePartiesPage page = new PModePartiesPage(driver);
		page.getSidebar().goToPage(PAGES.PMODE_PARTIES);
		
		soft.assertTrue(page.filters().getNameInput().isEnabled(), "Page contains filter for party name");
		soft.assertTrue(page.filters().getEndpointInput().isEnabled(), "Page contains filter for party endpoint");
		soft.assertTrue(page.filters().getPartyIDInput().isEnabled(), "Page contains filter for party id");
		soft.assertTrue(page.filters().getProcessRoleSelect().isDisplayed(), "Page contains filter for process role");
		
		List<HashMap<String, String>> partyInfo = page.grid().getAllRowInfo();
		soft.assertTrue(partyInfo.size() == 2, "Grid contains both the parties described in PMode file");
		
		soft.assertTrue(!page.getCancelButton().isEnabled(), "Cancel button is NOT enabled");
		soft.assertTrue(!page.getSaveButton().isEnabled(), "Save button is NOT enabled");
		soft.assertTrue(!page.getEditButton().isEnabled(), "Edit button is NOT enabled");
		soft.assertTrue(!page.getDeleteButton().isEnabled(), "Delete button is NOT enabled");
		soft.assertTrue(page.getNewButton().isEnabled(), "New button is enabled");
		
		soft.assertAll();
		
	}
	
	@Test(description = "PMP-18", groups = {"multiTenancy", "singleTenancy"})
	public void selectRow() throws Exception {
		SoftAssert soft = new SoftAssert();
		
		rest.pmode().uploadPMode("pmodes/doNothingInvalidRed.xml", null);
		
		PModePartiesPage page = new PModePartiesPage(driver);
		page.getSidebar().goToPage(PAGES.PMODE_PARTIES);
		
		soft.assertTrue(!page.getEditButton().isEnabled(), "Edit button is not enabled");
		soft.assertTrue(!page.getDeleteButton().isEnabled(), "Delete button is not enabled");
		
		page.grid().selectRow(0);
		
		soft.assertTrue(page.getEditButton().isEnabled(), "Edit button is enabled after select row");
		soft.assertTrue(page.getDeleteButton().isEnabled(), "Delete button is enabled after select row");
		
		soft.assertAll();
	}
	
	@Test(description = "PMP-3", groups = {"multiTenancy", "singleTenancy"})
	public void downloadFilteredPartiesList() throws Exception {
		SoftAssert soft = new SoftAssert();
		
		log.info("Navigate to Pmode parties page");
		PModePartiesPage page = new PModePartiesPage(driver);
		page.getSidebar().goToPage(PAGES.PMODE_PARTIES);
		
		String partyName = page.grid().getRowInfo(0).get("Party Name");
		
		log.info("Search for party with id " + partyName);
		page.filters().getNameInput().fill(partyName);
		log.info("Click on search button");
		page.filters().getSearchButton().click();
		
		page.grid().waitForRowsToLoad();
		
		String filename = page.pressSaveCsvAndSaveFile();
		
		page.grid().checkCSVvsGridInfo(filename, soft);
		
		soft.assertAll();
	}
	
	@Test(description = "PMP-16", groups = {"multiTenancy", "singleTenancy"})
	public void downloadAllParties() throws Exception {
		SoftAssert soft = new SoftAssert();
		
		log.info("Navigate to Pmode parties page");
		PModePartiesPage page = new PModePartiesPage(driver);
		page.getSidebar().goToPage(PAGES.PMODE_PARTIES);
		
		page.grid().waitForRowsToLoad();
		
		String filename = page.pressSaveCsvAndSaveFile();
		
		page.grid().checkCSVvsGridInfo(filename, soft);
		
		soft.assertAll();
	}
     /* PMP-19 User single click on row on Pmode-Parties*/
	@Test(description = "PMP-19", groups = {"multiTenancy", "singleTenancy"})
	public void singleClick() throws Exception {

		SoftAssert soft = new SoftAssert();

		PModePartiesPage page = new PModePartiesPage(driver);
		page.getSidebar().goToPage(PAGES.PMODE_PARTIES);
		page.grid().waitForRowsToLoad();
		if(page.grid().getPagination().getTotalItems()>0)
		{
			page.grid().selectRow(0);
			soft.assertTrue(page.getEditButton().isEnabled(),"Edit button gets enabled");
			soft.assertTrue(page.getDeleteButton().isEnabled(),"Delete button gets enabled");
			soft.assertTrue(page.grid().gridRows.get(0).getAttribute("class").contains("active"),"Row gets highlighted");
		}
		else{
			throw new SkipException("No Parties for selection");
		}
		soft.assertAll();
	}

}
