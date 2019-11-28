package domibus.ui.functional;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.popups.Dialog;
import ddsl.enums.DMessages;
import ddsl.enums.PAGES;
import ddsl.enums.DRoles;
import utils.BaseTest;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import pages.login.LoginPage;
import pages.users.UserModal;
import pages.users.UsersPage;
import utils.Generator;
import utils.TestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


public class UsersPgTest extends BaseTest {

	ArrayList<String> ADMIN_VISIBLE_ROLES = new ArrayList<>(Arrays.asList(DRoles.ADMIN, DRoles.USER));

	ArrayList<String> SUPER_NEW_VISIBLE_ROLES = new ArrayList<>(Arrays.asList(DRoles.SUPER, DRoles.ADMIN, DRoles.USER));

	ArrayList<String> SUPER_EDIT_USER_VISIBLE_ROLES = new ArrayList<>(Arrays.asList(DRoles.ADMIN, DRoles.USER));
	ArrayList<String> SUPER_EDIT_ADMIN_VISIBLE_ROLES = new ArrayList<>(Arrays.asList(DRoles.ADMIN, DRoles.USER));
	ArrayList<String> SUPER_EDIT_SUPER_VISIBLE_ROLES = new ArrayList<>(Arrays.asList(DRoles.SUPER));

	private boolean testRoleList(List<String> expectedOptions, UserModal modal) throws Exception {
		log.info("getting visible options");
		List<String> visibleOptions = modal.getRoleSelect().getOptionsTexts();
		return TestUtils.isEqualListContent(visibleOptions, expectedOptions);
	}


	private UsersPage loginAndGoToUsersPage(HashMap<String, String> user) throws Exception {
		LoginPage loginPage = new LoginPage(driver);
		log.info("Login with user" + user);
		loginPage.login(user);

		log.info("Open Users page");
		loginPage.getSidebar().goToPage(PAGES.USERS);
		return new UsersPage(driver);
	}

	private UsersPage loginAndGoToUsersPage(String user, String pass) throws Exception {
		HashMap<String, String> userInfo = new HashMap<>();
		userInfo.put("username", user);
		userInfo.put("pass", pass);

		return loginAndGoToUsersPage(userInfo);
	}

	/* Admin deletes user and presses Save */
	@Test(description = "USR-10", groups = {"multiTenancy", "singleTenancy"})
	public void deleteUserAndSave() throws Exception {
		String username = getUser(null, DRoles.USER, true, false, false).getString("userName");

		SoftAssert soft = new SoftAssert();
		loginAndGoToUsersPage(data.getAdminUser());

		UsersPage page = new UsersPage(driver);

		log.info("Selecting user " + username);
		int index = page.grid().scrollTo("Username", username);
		page.grid().selectRow(index);

		log.info("Press Delete button");
		page.getDeleteBtn().click();

		soft.assertTrue(page.getSaveBtn().isEnabled(), "After pressing delete the Save button is active");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "After pressing delete the Cancel button is active");

		log.info("Save changes");
		page.saveAndConfirm();

		soft.assertTrue(page.getUsersGrid().isDeleted(username), "User presented as deleted in the grid");

		soft.assertAll();
	}

	/* Admin deletes user and presses Cancel */
	@Test(description = "USR-9", groups = {"multiTenancy", "singleTenancy"})
	public void deleteUserAndCancel() throws Exception {
		String username = getUser(null, DRoles.USER, true, false, false).getString("userName");

		SoftAssert soft = new SoftAssert();
		loginAndGoToUsersPage(data.getAdminUser());

		UsersPage page = new UsersPage(driver);

		log.info("Selecting user " + username);
		int index = page.grid().scrollTo("Username", username);
		page.grid().selectRow(index);

		log.info("Press Delete button");
		page.getDeleteBtn().click();

		soft.assertTrue(page.getSaveBtn().isEnabled(), "After pressing delete the Save button is active");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "After pressing delete the Cancel button is active");

		log.info("Cancel changes");
		page.cancelAndConfirm();

		soft.assertTrue(!page.getUsersGrid().isDeleted(username), "User presented as NOT deleted in the grid");

		soft.assertAll();
	}

	/* Available roles in Role drop down on new/edit pop up */
	@Test(description = "USR-12", groups = {"multiTenancy", "singleTenancy"})
	public void availableRolesAdmin() throws Exception {
		SoftAssert soft = new SoftAssert();

		String adminUser = getUser(null, DRoles.ADMIN, true, false, true).getString("userName");
		String toEditUser = getUser(null, DRoles.USER, true, false, false).getString("userName");

		log.info("got user " + toEditUser);
		log.info("got admin " + adminUser);

		login(adminUser, data.defaultPass()).getSidebar().goToPage(PAGES.USERS);
		UsersPage page = new UsersPage(driver);

		log.info("click NEW");
		page.getNewBtn().click();
		UserModal modal = new UserModal(driver);
		soft.assertTrue(testRoleList(ADMIN_VISIBLE_ROLES, modal), "Roles available for ADMIN");

		log.info("closing user modal");
		page.clickVoidSpace();

		log.info("editing user " + toEditUser);
		page.grid().scrollToAndSelect("Username", toEditUser);
		page.getEditBtn().click();
		soft.assertTrue(testRoleList(ADMIN_VISIBLE_ROLES, modal), "Roles available for ADMIN");

		log.info("closing user modal");
		page.clickVoidSpace();

		if(data.isIsMultiDomain()) {
			logout();
			String superUser = getUser(null, DRoles.SUPER, true, false, true).getString("userName");
			log.info("checking for super admin " +superUser);
			login(superUser, data.defaultPass()).getSidebar().goToPage(PAGES.USERS);

			log.info("click NEW");
			page.getNewBtn().click();
			soft.assertTrue(testRoleList(SUPER_NEW_VISIBLE_ROLES, modal), "All roles available for SUPER when creating new user");
			log.info("closing modal");
			page.clickVoidSpace();

			log.info("editing user " + toEditUser);
			page.grid().scrollToAndSelect("Username", toEditUser);
			page.getEditBtn().click();
			soft.assertTrue(testRoleList(SUPER_EDIT_USER_VISIBLE_ROLES, modal), "All roles available for SUPER when editing a user");
			log.info("closing modal");
			page.clickVoidSpace();

			log.info("editing admin " + adminUser);
			page.grid().scrollToAndSelect("Username", adminUser);
			page.getEditBtn().click();
			soft.assertTrue(testRoleList(SUPER_EDIT_ADMIN_VISIBLE_ROLES, modal), "All roles available for SUPER when editing an ADMIN");
			log.info("closing modal");
			page.clickVoidSpace();

			log.info("editing super user " + superUser);
			page.grid().scrollToAndSelect("Username", superUser);
			page.getEditBtn().click();
			soft.assertTrue(testRoleList(SUPER_EDIT_SUPER_VISIBLE_ROLES, modal), "All roles available for SUPER when editing an ADMIN");
			log.info("closing modal");
			page.clickVoidSpace();
		}

		soft.assertAll();
	}


	/* USR-4 - Create new user and press cancel */
	@Test(description = "USR-4", groups = {"multiTenancy", "singleTenancy"})
	public void newUserCancel() throws Exception {

		String username = Generator.randomAlphaNumeric(9);

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");

//		create new user
		log.info("creating new user " + username);
		page.newUser(username, "tuser@bnc.com", DRoles.ADMIN, data.defaultPass(), data.defaultPass());
		page.grid().waitForRowsToLoad();

		log.info("Press cancel");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is enabled after new user creation");
		page.cancelAndConfirm();

		log.info("searching for user in grid");
		int index = page.grid().scrollTo("Username", username);
		soft.assertEquals(index, -1, "User not present in the list of users");

		soft.assertAll();
	}

	/* USR-6 - Admin creates new user and presses Save */
	@Test(description = "USR-6", groups = {"multiTenancy", "singleTenancy"})
	public void newUserSave() throws Exception {

		String username = Generator.randomAlphaNumeric(9);

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled on page load");

//		create new user
		log.info("creating new user " + username);
		page.newUser(username, "tuser@bnc.com", DRoles.ADMIN, data.defaultPass(), data.defaultPass());
		page.grid().waitForRowsToLoad();

		log.info("Press Save");
		soft.assertTrue(page.getSaveBtn().isEnabled(), "Cancel button is enabled after new user creation");
		page.saveAndConfirm();

		log.info("searching for user in grid");
		int index = page.grid().scrollTo("Username", username);
		soft.assertTrue(index > -1, "User present in the list of users");

		soft.assertAll();
	}

	/* USR-7 - Admin edits an existing user and presses Cancel */
	@Test(description = "USR-7", groups = {"multiTenancy", "singleTenancy"})
	public void editUserAndCancel() throws Exception {
		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled on page load");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");

		log.info("aquiring user for edit");
		String username = getUser(null, DRoles.USER, false, false, false).getString("userName");
		page.refreshPage();

		log.info("editing user");
		page.grid().scrollToAndDoubleClick("Username", username);
		UserModal modal = new UserModal(driver);
		log.info("make the user active");
		modal.getActiveChk().check();

		String email = Generator.randomAlphaNumeric(5) + "@test.com";
		log.info("editign email to " + email);
		modal.getEmailInput().fill(email);
		modal.clickOK();

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is enabled after edit");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is enabled after edit");

		log.info("click Cancel");
		page.cancelAndConfirm();

		log.info("checking edited values");
		page.grid().scrollToAndDoubleClick("Username", username);
		modal = new UserModal(driver);

		soft.assertTrue(!modal.getActiveChk().isChecked(), "User is still disabled");
		soft.assertNotEquals(modal.getEmailInput().getText(), email, "User email is NOT the one set by editing");

		soft.assertAll();
	}

	/* USR-7 - Admin edits an existing user and presses Save */
	@Test(description = "USR-8", groups = {"multiTenancy", "singleTenancy"})
	public void editUserAndSave() throws Exception {
		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		soft.assertTrue(!page.getSaveBtn().isEnabled(), "Save button is disabled on page load");
		soft.assertTrue(!page.getCancelBtn().isEnabled(), "Cancel button is disabled on page load");

		log.info("aquiring user for edit");
		String username = getUser(null, DRoles.USER, false, false, false).getString("userName");
		page.refreshPage();

		log.info("editing user");
		page.grid().scrollToAndDoubleClick("Username", username);
		UserModal modal = new UserModal(driver);
		log.info("make the user active");
		modal.getActiveChk().check();

		String email = Generator.randomAlphaNumeric(5) + "@test.com";
		log.info("editign email to " + email);
		modal.getEmailInput().fill(email);
		modal.clickOK();

		soft.assertTrue(page.getSaveBtn().isEnabled(), "Save button is enabled after edit");
		soft.assertTrue(page.getCancelBtn().isEnabled(), "Cancel button is enabled after edit");

		log.info("click Save");
		page.saveAndConfirm();

		log.info("checking edited values");
		page.grid().scrollToAndDoubleClick("Username", username);
//		modal = new UserModal(driver);

		soft.assertTrue(modal.getActiveChk().isChecked(), "User is enabled");
		soft.assertEquals(modal.getEmailInput().getText(), email, "User email is the one set by editing");

		soft.assertAll();
	}

	/* USR-15 - Admin unticks the Active checkbox for user (also applies to user creation) */
	@Test(description = "USR-15", groups = {"multiTenancy", "singleTenancy"})
	public void adminDeactivatesUser() throws Exception {
		SoftAssert soft = new SoftAssert();
		String username = Generator.randomAlphaNumeric(10);
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

//		admin creates new disabled user
		log.info("Create new disabled user");
		page.getNewBtn().click();

		log.info("Creating user " + username);
		UserModal modal = new UserModal(driver);
		modal.fillData(username, null, DRoles.USER, data.defaultPass(), data.defaultPass());

		log.info("Uncheck the active checkbox");
		modal.getActiveChk().uncheck();
		modal.clickOK();

		log.info("Saving");
		page.saveAndConfirm();

		log.info("logging out");
		page.getSandwichMenu().logout();
		DomibusPage pg = login(username, data.defaultPass());
		soft.assertTrue(pg.getAlertArea().isError(), "Error displayed for deactivated user trying to login");
		soft.assertEquals(pg.getAlertArea().getAlertMessage(), DMessages.LOGIN_USER_INACTIVE, "User is notified that his account is inactive");

//		admin edits to disable active user
		log.info("Disable active user");
		username = getUser(null, DRoles.USER, true, false, false).getString("userName");

		page = loginAndGoToUsersPage(data.getAdminUser());

		log.info("editing user " + username);
		page.grid().scrollToAndDoubleClick("Username", username);
		modal = new UserModal(driver);

		log.info("Uncheck the active checkbox");
		modal.getActiveChk().uncheck();
		modal.clickOK();

		log.info("Saving");
		page.saveAndConfirm();

		log.info("logging out");
		page.getSandwichMenu().logout();

		pg = login(username, data.defaultPass());
		soft.assertTrue(pg.getAlertArea().isError(), "Error displayed for deactivated user trying to login");
		soft.assertEquals(pg.getAlertArea().getAlertMessage(), DMessages.LOGIN_USER_INACTIVE, "User is notified that his account is inactive");

		soft.assertAll();
	}

	/* USR-14 - Admin changes password (also applies to user creation) */
	@Test(description = "USR-14", groups = {"multiTenancy", "singleTenancy"})
	public void adminChangesUserPassword() throws Exception {
		SoftAssert soft = new SoftAssert();
		String username = getUser(null, DRoles.USER, true, false, true).getString("userName");
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		log.info("changing password for " + username);
		page.grid().scrollToAndDoubleClick("Username", username);
		UserModal modal = new UserModal(driver);
		modal.getPasswordInput().fill(data.getNewTestPass());
		modal.getConfirmationInput().fill(data.getNewTestPass());
		modal.clickOK();

		log.info("Saving");
		page.saveAndConfirm();

		log.info("admin logging out");
		page.getSandwichMenu().logout();

		log.info("try to login with old password");
		DomibusPage pg = login(username, data.defaultPass());
		soft.assertTrue(pg.getAlertArea().isError(), "Error displayed when trying to login with old password");
		soft.assertEquals(pg.getAlertArea().getAlertMessage(), DMessages.LOGIN_INVALID_CREDENTIALS, "User notified he has wrong credentials");

		log.info("try to login with new password");
		pg = login(username, data.getNewTestPass());
		soft.assertTrue(pg.getSandwichMenu().isLoggedIn(), "User can login with new pass");

		soft.assertAll();
	}

	/*USR-16 - Admin tries to create new user with username less than 3 letters long*/
	@Test(description = "USR-16", groups = {"multiTenancy", "singleTenancy"}, enabled = false)
	public void userNameValidations() throws Exception {
		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		log.info("click New");
		page.getNewBtn().click();


		UserModal modal = new UserModal(driver);
		log.info("checking with only one letter");
		modal.getUserNameInput().fill("t");
		soft.assertEquals(modal.getUsernameErrMess().getText(), DMessages.USER_USERNAME_VALIDATION, "Correct error message shown (1)");

		log.info("checking with only two letters");
		modal.getUserNameInput().fill("te");
		soft.assertEquals(modal.getUsernameErrMess().getText(), DMessages.USER_USERNAME_VALIDATION, "Correct error message shown (2)");

		log.info("checking with only two letters and special characters");
		modal.getUserNameInput().fill("te$%^*");
		soft.assertEquals(modal.getUsernameErrMess().getText(), DMessages.USER_USERNAME_VALIDATION, "Correct error message shown (3)");

		log.info("checking with valid username");
		modal.getUserNameInput().fill("testUser");

		String errMess = null;
		try {
			errMess = modal.getUsernameErrMess().getText();
		} catch (Exception e) {
		}
		soft.assertNull(errMess, "When correct username is entered the error message dissapears");

		soft.assertAll();
	}

	/*USR-17 - Admin changes a user role*/
	@Test(description = "USR-17", groups = {"multiTenancy", "singleTenancy"})
	public void editUserRoleAndCheckPrivileges() throws Exception {
		// we need to create a new user, because a random existing one may have a different password
		String username = getUser(null, DRoles.ADMIN, true, false, false).getString("userName");
		rest.createUser(Generator.randomAlphaNumeric(10), DRoles.ADMIN, data.defaultPass(), null);

		log.info("changing role to User for Admin " + username);

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

		log.info("editing user:" + username);
		page.grid().scrollToAndDoubleClick("Username", username);

		log.info("changing role");
		UserModal um = new UserModal(driver);
		um.getRoleSelect().selectOptionByText(DRoles.USER);
		um.clickOK();

		log.info("Saving");
		page.saveAndConfirm();

		log.info("logout");
		page.getSandwichMenu().logout();

		log.info("login with username " + username);
		LoginPage loginPage = new LoginPage(driver);
		loginPage.login(username, data.defaultPass());

		soft.assertEquals(loginPage.getSidebar().availableOptions().size(), 2, "User has only 2 options available in sidebar");

		// TODO: add other combinations of privileges (User to Admin, Super to Admin and viceversa, Super to user and reverse)

		rest.deleteUser(username, null);
		soft.assertAll();
	}

	/*USR-19 - Admin tries to create a user with username that exists already (active deleted) both*/
	@Test(description = "USR-19", groups = {"multiTenancy", "singleTenancy"})
	public void duplicateUsername() throws Exception {
		String username = getUser(null, DRoles.USER, false, false, false).getString("userName");
		String deleted_username = getUser(null, DRoles.USER, false, true, false).getString("userName");

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

//		active user
		log.info("creating new user with existing active username");
		page.getNewBtn().click();
		UserModal um = new UserModal(driver);
		um.fillData(username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		log.info("Saving");
		page.getSaveBtn().click();

		log.info("checking error message");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		soft.assertEquals(page.getAlertArea().getAlertMessage(), String.format(DMessages.Users.DUPLICATE_USERNAME_SAMEDOMAIN_ERROR, username), "Correct message displayed");

//		deleted user
		log.info("creating new user with existing deleted username");
		page.getNewBtn().click();
		um = new UserModal(driver);
		um.fillData(deleted_username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		log.info("Saving");
		page.getSaveBtn().click();

		log.info("checking error message");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		soft.assertEquals(page.getAlertArea().getAlertMessage(), String.format(DMessages.Users.DUPLICATE_USERNAME_SAMEDOMAIN_ERROR, username), "Correct message displayed");

		soft.assertAll();
	}

	/*USR-20 - Admin tries to create a user with username that exists on another domain*/
	@Test(description = "USR-20", groups = {"multiTenancy"})
	public void duplicateUsernameOnAnotherDomain() throws Exception {
		String domainName = getNonDefaultDomain();
		String domainCode = rest.getDomainCodeForName(domainName);
		String username = getUser(domainCode, DRoles.USER, false, false, false).getString("userName");
		String deleted_username = getUser(domainCode, DRoles.USER, false, true, false).getString("userName");

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(data.getAdminUser());

//		active user
		log.info("creating new user with existing active username");
		page.getNewBtn().click();
		UserModal um = new UserModal(driver);
		um.fillData(username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		log.info("Saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking error message");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		soft.assertEquals(page.getAlertArea().getAlertMessage(), String.format(DMessages.Users.DUPLICATE_USERNAME_ERROR, username, domainCode), "Correct message displayed");

//		deleted user
		log.info("creating new user with existing deleted username");
		page.getNewBtn().click();
		um = new UserModal(driver);
		um.fillData(deleted_username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		log.info("Saving");
		page.getSaveBtn().click();
		new Dialog(driver).confirm();

		log.info("checking error message");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		soft.assertEquals(page.getAlertArea().getAlertMessage(), String.format(DMessages.Users.DUPLICATE_USERNAME_ERROR, deleted_username, domainCode), "Correct message displayed");


		soft.assertAll();
	}

	/*USR-21 - Admin tries to create a user with username that exists on a Plugin user*/
	@Test(description = "USR-21", groups = {"multiTenancy"})
	public void duplicateUserVSPluginUser() throws Exception {

		String username = getPluginUser(null, DRoles.ADMIN, true, false).getString("userName");
		log.info("got plugin user " + username);

		SoftAssert soft = new SoftAssert();
		loginAndGoToUsersPage(data.getAdminUser());
		UsersPage page = new UsersPage(driver);

		log.info("creating new user");
		page.getNewBtn().click();
		UserModal um = new UserModal(driver);
		um.fillData(username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		page.saveAndConfirm();

		log.info("checking");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		String expectedMessage = String.format(DMessages.USER_DUPLICATE_USERNAME, username, "default");
		soft.assertEquals(page.getAlertArea().getAlertMessage(), expectedMessage, "Correct message displayed");

		soft.assertAll();
	}

	/*USR-22 - Admin tries to create a user with username that exists on a Plugin user on another domain*/
	@Test(description = "USR-22", groups = {"multiTenancy"})
	public void duplicateUserVSPluginUserOtherDomain() throws Exception {
		String domainName = getNonDefaultDomain();
		String domainCode = rest.getDomainCodeForName(domainName);
		String username = getPluginUser(domainCode, DRoles.ADMIN, true, false).getString("userName");
		log.info("got plugin user " + username + "on domain " + domainCode);

		SoftAssert soft = new SoftAssert();
		loginAndGoToUsersPage(data.getAdminUser());
		UsersPage page = new UsersPage(driver);

		log.info("creating new user");
		page.getNewBtn().click();
		UserModal um = new UserModal(driver);
		um.fillData(username, "", DRoles.USER, data.defaultPass(), data.defaultPass());
		um.clickOK();

		page.saveAndConfirm();

		log.info("checking");
		soft.assertEquals(page.getAlertArea().isError(), true, "Error message displayed");
		String expectedMessage = String.format(DMessages.USER_DUPLICATE_USERNAME, username, domainCode);
		soft.assertEquals(page.getAlertArea().getAlertMessage(), expectedMessage, "Correct message displayed");

		soft.assertAll();
	}

	/*USR-32 - Delete logged in user*/
	@Test(description = "USR-32", groups = {"multiTenancy", "singleTenancy"})
	public void adminDeleteSelf() throws Exception {
		String username = getUser(null, DRoles.ADMIN, true, false, true).getString("userName");
		log.info("created user " + username);

		SoftAssert soft = new SoftAssert();
		UsersPage page = loginAndGoToUsersPage(username, data.defaultPass());

		log.info("deleteing created user");
		page.grid().scrollToAndSelect("Username", username);
		page.getDeleteBtn().click();

		soft.assertTrue(page.getAlertArea().isError(), "Error message is shown");
		soft.assertEquals(page.getAlertArea().getAlertMessage(),
				String.format(DMessages.USER_DELETE_LOGGED_IN_USER, username),
				"Correct error message is shown");

		soft.assertAll();
	}





}
