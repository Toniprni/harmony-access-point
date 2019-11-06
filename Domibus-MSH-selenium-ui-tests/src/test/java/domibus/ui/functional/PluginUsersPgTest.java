package domibus.ui.functional;

import ddsl.dcomponents.grid.DGrid;
import ddsl.dcomponents.popups.Dialog;
import ddsl.enums.DMessages;
import ddsl.enums.PAGES;
import ddsl.enums.DRoles;
import domibus.BaseTest;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.plugin_users.CertPluginUserModal;
import pages.plugin_users.PluginUserModal;
import pages.plugin_users.PluginUsersPage;
import utils.Generator;
import utils.TestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


public class PluginUsersPgTest extends BaseTest {

	JSONObject descriptorObj = TestUtils.getPageDescriptorObject(PAGES.PLUGIN_USERS);

	/*	PU-3 - Create new user and press Cancel	*/
	@Test(description = "PU-3", groups = {"multiTenancy", "singleTenancy"})
	public void newUserCancel() throws Exception {

		String username = Generator.randomAlphaNumeric(9);
		log.info("creating user " + username);
		SoftAssert soft = new SoftAssert();

		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("checking Cancel button state");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");

//		create new user
		log.info("filling form for new user " + username);
		page.newUser(username, DRoles.ADMIN, data.defaultPass(), data.defaultPass());
		page.grid().waitForRowsToLoad();

		log.info("checking Cancel button state");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is enabled after new user creation");

		log.info("click Cancel");
		page.getCancelBtn().click();
		new Dialog(driver).confirm();

		log.info("checking grid for user " + username);
		int index = page.grid().scrollTo("User Name", username);
		soft.assertEquals(index, -1, "User not present in the list of users");

		soft.assertAll();
	}

	/*	PU-5 - Admin creates new user and presses Save	*/
	@Test(description = "PU-5", groups = {"multiTenancy", "singleTenancy"})
	public void newUserSave() throws Exception {

		String username = Generator.randomAlphaNumeric(9);
		log.info("creating user " + username);

		SoftAssert soft = new SoftAssert();

		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("checking buttons state");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");
		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled on page load");

//		create new user
		log.info("filling plugin user form");
		page.newUser(username, DRoles.ADMIN, data.defaultPass(), data.defaultPass());

		page.grid().waitForRowsToLoad();
		log.info("checking buttons state");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is enabled after new user creation");
		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is enabled after new user creation");

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();
		page.clickVoidSpace();

		DGrid grid = page.grid();
		grid.waitForRowsToLoad();

		log.info("checking grid for user " + username);
		int index = page.grid().scrollTo("User Name", username);
		soft.assertTrue(index > -1, "Created user present in grid");

		log.info("checking buttons state");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled after new user persisted");
		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled after new user persisted");

		rest.deletePluginUser(username, null);

		soft.assertAll();
	}

	/*	PU-16 - Admin changes a user role	*/
	@Test(description = "PU-16", groups = {"multiTenancy", "singleTenancy"})
	public void editUserRole() throws Exception {
		String username = getPluginUser(null, DRoles.USER, true, false).getString("userName");
		log.info("editing user " + username);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		DGrid grid = page.grid();
		log.info("selecting user " + username);
		grid.scrollToAndSelect("User Name", username);
		page.getEditBtn().click();

		PluginUserModal pum = new PluginUserModal(driver);

		log.info("change role");
		pum.getRolesSelect().selectOptionByText(DRoles.ADMIN);
		pum.clickOK();

		log.info("check grid for updated info");
		HashMap<String, String> info = grid.getRowInfo("User Name", username);
		soft.assertEquals(info.get("Role"), DRoles.ADMIN, "role is updated");

		log.info("click Save");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("check grid for updated info");
		info = grid.getRowInfo("User Name", username);
		soft.assertEquals(info.get("Role"), DRoles.ADMIN, "role is updated");

		soft.assertAll();
	}

	/*	PU-6 - Admin edits an existing user and presses Cancel	*/
	@Test(description = "PU-6", groups = {"multiTenancy", "singleTenancy"})
	public void editAndCancel() throws Exception {

		String toAdd = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C7";
		String username = getPluginUser(null, DRoles.USER, true, false).getString("userName");
		log.info("editing user " + username);

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);

		DGrid grid = page.grid();
		log.info("selecting user " + username);
		grid.scrollToAndSelect("User Name", username);
		page.getEditBtn().click();

		PluginUserModal pum = new PluginUserModal(driver);

		log.info("fill Original User input with invalid string");
		pum.getOriginalUserInput().fill("testEdit");
		log.info("check error message");
		soft.assertTrue(!pum.getOkBtn().isEnabled(), "Invalid value cannot be saved in the Original User field");

		log.info("fill Original User input with valid string");
		pum.getOriginalUserInput().fill(toAdd);
		pum.clickOK();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("Original User", toAdd) > -1, "Edited value is visible in the grid");

		log.info("click cancel");
		page.getCancelBtn().click();
		new Dialog(driver).confirm();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("Original User", toAdd) == -1, "Edited value is NOT visible in the grid after Cancel");

		soft.assertAll();

	}

	/* PU-7 - Admin edits an existing user and presses Save */
	@Test(description = "PU-7", groups = {"multiTenancy", "singleTenancy"})
	public void editAndSave() throws Exception {
		String toAdd = "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C7";
		String username = getPluginUser(null, DRoles.USER, true, false).getString("userName");
		log.info("editing user " + username);

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);

		DGrid grid = page.grid();
		log.info("selecting user " + username);
		grid.scrollToAndSelect("User Name", username);
		page.getEditBtn().click();

		PluginUserModal pum = new PluginUserModal(driver);

		log.info("fill Original User input with invalid string");
		pum.getOriginalUserInput().fill("testEdit");
		log.info("check error message");
		soft.assertTrue(!pum.getOkBtn().isEnabled(), "Invalid value cannot be saved in the Original User field");

		log.info("fill Original User input with valid string");
		pum.getOriginalUserInput().fill(toAdd);
		pum.clickOK();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("Original User", toAdd) > -1, "Edited value is visible in the grid");

		log.info("click Save");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("Original User", toAdd) > -1, "Edited value is visible in the grid after Save");

		soft.assertAll();

	}

	/*	PU-8 - Admin deletes user and presses Cancel	*/
	@Test(description = "PU-8", groups = {"multiTenancy", "singleTenancy"})
	public void deleteAndCancel() throws Exception {
		String username = getPluginUser(null, DRoles.USER, true, false).getString("userName");
		log.info("testing for user " + username);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);
		PluginUsersPage page = new PluginUsersPage(driver);

		DGrid grid = page.grid();
		log.info("selecting user " + username);
		grid.scrollToAndSelect("User Name", username);

		log.info("click delete ");
		page.getDeleteBtn().click();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("User Name", username) == -1, "Plugin user is not present in the grid after delete");

		log.info("canceling");
		page.getCancelBtn().click();
		new Dialog(driver).confirm();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("User Name", username) > -1, "Plugin user is present in the grid after Cancel");

		soft.assertAll();
	}

	/*	PU-9 - Admin deletes user and presses Save	*/
	@Test(description = "PU-9", groups = {"multiTenancy", "singleTenancy"})
	public void deleteAndSave() throws Exception {
		String username = getPluginUser(null, DRoles.USER, true, false).getString("userName");
		log.info("testing for user " + username);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);
		PluginUsersPage page = new PluginUsersPage(driver);

		DGrid grid = page.grid();
		log.info("selecting user " + username);
		grid.scrollToAndSelect("User Name", username);

		log.info("click delete ");
		page.getDeleteBtn().click();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("User Name", username) == -1, "Plugin user is not present in the grid after delete");

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("check grid for updated info");
		soft.assertTrue(grid.scrollTo("User Name", username) == -1, "Plugin user is NOT present in the grid after Save");

		soft.assertAll();
	}

	/*PU-13 - Create a certificate plugin userand press save*/
	@Test(description = "PU-13", groups = {"multiTenancy", "singleTenancy"})
	public void createCertificatePluginUserSave() throws Exception {
		String id = Generator.randomAlphaNumeric(5);
		String certId = "CN=puser,O=eDelivery,C=BE:" + id;
		log.info("creating plugin user with certificate " + certId);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);
		PluginUsersPage page = new PluginUsersPage(driver);

		log.info("switching to auth type certificate");
		page.filters.getAuthTypeSelect().selectOptionByText("CERTIFICATE");
		page.grid().waitForRowsToLoad();

		log.info("click new user button");
		page.getNewBtn().click();

		log.info("adding data in form");
		CertPluginUserModal modal = new CertPluginUserModal(driver);
		modal.getUserInput().fill(certId);
		modal.getRoleSelect().selectOptionByText(DRoles.ADMIN);
		modal.clickOK();

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking grid for new data");
		soft.assertTrue(page.grid().scrollTo("Certificate Id", certId) > -1, "New user is present in the grid");

		soft.assertAll();
	}

	/*PU-14 - Create a certificate plugin userand press cancel*/
	@Test(description = "PU-14", groups = {"multiTenancy", "singleTenancy"})
	public void createCertificatePluginUserCancel() throws Exception {
		String id = Generator.randomAlphaNumeric(5);
		String certId = "CN=puser,O=eDelivery,C=BE:" + id;
		log.info("creating plugin user with certificate " + certId);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("switching to auth type certificate");
		page.filters.getAuthTypeSelect().selectOptionByText("CERTIFICATE");
		page.grid().waitForRowsToLoad();

		log.info("click new user button");
		page.getNewBtn().click();

		log.info("adding data in form");
		CertPluginUserModal modal = new CertPluginUserModal(driver);
		modal.getUserInput().fill(certId);
		modal.getRoleSelect().selectOptionByText(DRoles.ADMIN);
		modal.clickOK();

		log.info("canceling");
		page.getCancelBtn().click();
		new Dialog(driver).confirm();

		log.info("checking grid for data");
		soft.assertTrue(page.grid().scrollTo("Certificate Id", certId) == -1, "New user is NOT present in the grid");

		soft.assertAll();
	}

	/*PU-18 - Admin tries to create users with the same username on multiple domains*/
	@Test(description = "PU-18", groups = {"multiTenancy"})
	public void duplicatePluginUsersDifferentDomain() throws Exception {
		String domain1 = getNonDefaultDomain();
		if (StringUtils.isEmpty(domain1)) {
			throw new SkipException("could not get domains");
		}
		String username = getPluginUser(domain1, DRoles.USER, true, false).getString("userName");
		log.info("testing for username " + username);

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("creating new user on default domain");
		page.newUser(username, DRoles.USER, data.defaultPass(), data.defaultPass());
		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking for error message");
		soft.assertTrue(page.getAlertArea().isError(), "Error message is shown");
		soft.assertEquals(page.getAlertArea().getAlertMessage(),
				String.format(DMessages.PLUGINUSER_DUPLICATE_USERNAME, username, domain1),
				"Error message is shown");

		rest.deletePluginUser(username, domain1);
		soft.assertAll();
	}

	/* PU-19 - Admin tries to create plugin user with the same name as a normal user from his domain */
	@Test(description = "PU-19", groups = {"multiTenancy"})
	public void sameUsernameAsUserOnSameDomain() throws Exception {
		String username = getUser(null, DRoles.USER, true, false, false).getString("userName");
		log.info("testing for user " + username);

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);

		log.info("adding new plugin user with username " + username);
		page.newUser(username, DRoles.USER, data.defaultPass(), data.defaultPass());
		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking page for errors");
		soft.assertTrue(page.getAlertArea().isError(), "Error message is shown");
		soft.assertEquals(page.getAlertArea().getAlertMessage(),
				String.format(DMessages.PLUGINUSER_DUPLICATE_USERNAME, username, "default"),
				"Error message is shown");

		soft.assertAll();
	}

	/*	PU-20 - Admin tries to create plugin user with the same name as a normal user from another domain	*/
	@Test(description = "PU-20", groups = {"multiTenancy"})
	public void sameUsernameAsUserOnDifferentDomain() throws Exception {
		String domain1 = getNonDefaultDomain();
		if (StringUtils.isEmpty(domain1)) {
			throw new SkipException("could not get domains");
		}
		String username = getUser(domain1, DRoles.USER, true, false, false).getString("userName");
		log.info("testing for username " + username);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);
		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("creating new user");
		page.newUser(username, DRoles.USER, data.defaultPass(), data.defaultPass());

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking page for errors");
		soft.assertTrue(page.getAlertArea().isError(), "Error message is shown");
		soft.assertEquals(page.getAlertArea().getAlertMessage(),
				String.format(DMessages.PLUGINUSER_DUPLICATE_USERNAME, username, domain1),
				"Error message is shown");

		soft.assertAll();
	}

	/* PU-31 - Check duplicate user addition with same certificate id  */
	@Test(description = "PU-31", groups = {"multiTenancy", "singleTenancy"})
	public void certificatePluginUserDuplicateSameDomain() throws Exception {
		String id = Generator.randomAlphaNumeric(5);
		String certId = "CN=puser,O=eDelivery,C=BE:" + id;
		log.info("testing for certificate id " + certId);

		SoftAssert soft = new SoftAssert();
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		page.filters.getAuthTypeSelect().selectOptionByText("CERTIFICATE");
		page.grid().waitForRowsToLoad();

		log.info("click new");
		page.getNewBtn().click();

		log.info("creating user");
		CertPluginUserModal modal = new CertPluginUserModal(driver);
		modal.getUserInput().fill(certId);
		modal.getRoleSelect().selectOptionByText(DRoles.ADMIN);
		modal.clickOK();

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("clicking new");
		page.getNewBtn().click();

		log.info("creating the same user");
		modal = new CertPluginUserModal(driver);
		modal.getUserInput().fill(certId);
		modal.getRoleSelect().selectOptionByText(DRoles.ADMIN);
		modal.clickOK();

		log.info("saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking for errors");
		soft.assertTrue(page.getAlertArea().isError(), "Page shows error message");

		soft.assertAll();
	}

	@Test(description = "*****", groups = {"multiTenancy", "singleTenancy"})
	public void duplicatePluginUsersSameDomain() throws Exception {
		String username = Generator.randomAlphaNumeric(10);
		rest.createPluginUser(username, DRoles.USER, data.defaultPass(), null);

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		page.newUser(username, DRoles.USER, data.defaultPass(), data.defaultPass());
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		soft.assertTrue(page.getAlertArea().isError(), "Error message is shown");
		soft.assertEquals(page.getAlertArea().getAlertMessage(),
				String.format(DMessages.PLUGINUSER_DUPLICATE_USERNAME, username, "default"),
				"Error message is shown");

		rest.deletePluginUser(username, null);
		soft.assertAll();
	}

	@Test(description = "*****", groups = {"multiTenancy"})
	public void domainVisibility() throws Exception {
		String username = Generator.randomAlphaNumeric(10);

		List<String> domains = rest.getDomainNames();
		String domain1 = "";
		for (String domain : domains) {
			if (!StringUtils.equalsIgnoreCase(domain, "Default")) {
				domain1 = domain;
				break;
			}
		}
		rest.createPluginUser(username, DRoles.USER, data.defaultPass(), rest.getDomainCodeForName(domain1));

		SoftAssert soft = new SoftAssert();
//		login with Admin and go to plugin users page
		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);

		soft.assertTrue(page.grid().scrollTo("User Name", username) == -1, "Plugin user is not visible on default domain");

		page.getDomainSelector().selectOptionByText(domain1);

		soft.assertTrue(page.grid().scrollTo("User Name", username) > -1, "Plugin user is visible on domain1");


		rest.deletePluginUser(username, domain1);
		soft.assertAll();
	}


	/*	PU-32 - Create duplicate plugin users by smashing the save button multiple times 	*/
	@Test(description = "PU-32", groups = {"multiTenancy", "singleTenancy"})
	public void newUserSaveMultipleSaves() throws Exception {

		String username = Generator.randomAlphaNumeric(9);
		log.info("creating user " + username);

		SoftAssert soft = new SoftAssert();

		login(data.getAdminUser()).getSidebar().goToPage(PAGES.PLUGIN_USERS);

		PluginUsersPage page = new PluginUsersPage(driver);
		log.info("checking buttons state");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");
		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled on page load");

//		create new user
		log.info("filling plugin user form");
		page.newUser(username, DRoles.ADMIN, data.defaultPass(), data.defaultPass());

		for (int i = 0; i < 10; i++) {
			try {
				log.info("saving");
				page.saveBtn.click();
				new Dialog(driver).yesBtn.click();
			} catch (Exception e) {	}
		}

		page.grid().waitForRowsToLoad();

		List<String> knownUsernames = new ArrayList<>();
		List<HashMap<String, String>> allInfo = page.grid().getAllRowInfo();
		for (HashMap<String, String> info : allInfo) {
			String pluginUser = info.get("User Name");
			soft.assertTrue(!knownUsernames.contains(pluginUser), "Username already present in list: " + pluginUser);
			knownUsernames.add(pluginUser);
		}

		soft.assertAll();
	}

}
