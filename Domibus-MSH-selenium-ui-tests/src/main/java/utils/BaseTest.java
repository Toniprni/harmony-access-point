package utils;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.FilterArea;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dobjects.DButton;
import ddsl.dobjects.DObject;
import ddsl.enums.DRoles;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.login.LoginPage;
import rest.DomibusRestClient;
import utils.driver.DriverManager;
import utils.soap_client.DomibusC1;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author Catalin Comanici
 * @version 4.1
 */
@Listeners(utils.customReporter.FailListener.class)
public class BaseTest {

	public static WebDriver driver;
	public static TestRunData data = new TestRunData();
	public static DomibusRestClient rest = new DomibusRestClient();
	public static DomibusC1 messageSender = new DomibusC1();

	public Logger log = LoggerFactory.getLogger(this.getClass().getName());

	public String logFilename;
//	private void makeLoggerLog() throws Exception{
//		PatternLayout layout = new PatternLayout("%d{ISO8601} [%C.%M] - %m%n");
//
//		Path file = Files.createTempFile("autoTests", ".txt");
//		this.logFilename = file.toAbsolutePath().toString();
//
//		FileAppender fileAppender = new FileAppender();
//		fileAppender.setFile(file.toAbsolutePath().toString());
//		fileAppender.setAppend(true);
//		fileAppender.setImmediateFlush(true);
//		fileAppender.setLayout(layout);
//		fileAppender.setName("CustomAppender");
//		fileAppender.setWriter(new BufferedWriter(new FileWriter(logFilename)));
//		log.addAppender(fileAppender);
//	}


	@BeforeSuite(alwaysRun = true)
	public void beforeSuite() throws Exception {
//		makeLoggerLog();
		log.info("Log file name is " + logFilename);
		log.info("-------- Starting -------");
		generateTestData();
	}

	@BeforeClass(alwaysRun = true)
	public void beforeClass() throws Exception {
		log.info("--------Initialize test class-------");

		driver = DriverManager.getDriver();
		driver.get(data.getUiBaseUrl());

	}

	@BeforeMethod(alwaysRun = true)
	protected void logSeparator() throws Exception {
		log.info("---------------------------");
		login(data.getAdminUser());
	}


	@AfterMethod(alwaysRun = true)
	protected void logout() throws Exception {
		DomibusPage page = new DomibusPage(driver);


		driver.manage().deleteAllCookies();
		((JavascriptExecutor) driver).executeScript("localStorage.clear();");

		/*refresh will close any remaining opened modals*/
		page.refreshPage();
		if (page.getSandwichMenu().isLoggedIn()) {
			log.info("Logging out");
			page.getSandwichMenu().logout();
		}
	}


	@AfterClass(alwaysRun = true)
	protected void afterClass() throws Exception {
		log.info("-------- Quitting driver after test class-------");
		try {
			driver.quit();
		} catch (Exception e) {
			log.warn("Closing the driver failed");
			e.printStackTrace();
		}
	}


	protected DomibusPage login(HashMap<String, String> user) {
		log.info("login started");
		LoginPage loginPage = new LoginPage(driver);

		try {

			if(loginPage.getSandwichMenu().isLoggedIn()){
				String currentLoggedInUser = loginPage.getSandwichMenu().getCurrentUserID();
				if(StringUtils.equalsIgnoreCase(currentLoggedInUser, user.get("username"))){
					loginPage.refreshPage();
					return new DomibusPage(driver);
				}
				logout();
			}

			loginPage.login(user);
			loginPage.waitForTitle();
		} catch (Exception e) {
			log.info("Login did not succeed!!!");
			log.debug(e.getMessage());
		}

		return new DomibusPage(driver);
	}

	protected DomibusPage login(String user, String pass) {

		HashMap<String, String> userInfo = new HashMap<>();
		userInfo.put("username", user);
		userInfo.put("pass", pass);

		login(userInfo);

		return new DomibusPage(driver);
	}


	public List<String> getMessageIDs(String domainCode, int noOfNecessaryMessages, boolean forceNew) throws Exception {
		JSONArray mess = rest.getListOfMessages(domainCode);
		List<String> messIDs = new ArrayList<>();

		if (forceNew) {
			return sendMessages(noOfNecessaryMessages, domainCode);
		}

		if (mess.length() < noOfNecessaryMessages) {
			List<String> sentMess = sendMessages(noOfNecessaryMessages - mess.length(), domainCode);
			messIDs.addAll(sentMess);
		}

		for (int i = 0; i < mess.length(); i++) {
			messIDs.add(mess.getJSONObject(i).getString("messageId"));
		}

		return messIDs;
	}

	public List<String> getMessageIDsWithStatus(String domainCode, String status) throws Exception {
		JSONArray mess = rest.getListOfMessages(domainCode);
		List<String> messIDs = new ArrayList<>();

		for (int i = 0; i < mess.length(); i++) {
			if(mess.getJSONObject(i).getString("messageStatus").equalsIgnoreCase(status))
			messIDs.add(mess.getJSONObject(i).getString("messageId"));
		}

		return messIDs;
	}

	public List<String> sendMessages(int noOf, String domainCode) throws Exception {
		List<String> messIDs = new ArrayList<>();

		String user = Generator.randomAlphaNumeric(10);
		String messageRefID = Generator.randomAlphaNumeric(10);
		String conversationID = Generator.randomAlphaNumeric(10);

		rest.createPluginUser(user, DRoles.ADMIN, data.defaultPass(), domainCode);
		log.info("Created plugin user " + user + " on domain " + domainCode);

		log.info("Uploading PMODE ");
		rest.uploadPMode("pmodes/pmode-blue.xml", null);

		for (int i = 0; i < noOf; i++) {
			messIDs.add(messageSender.sendMessage(user, data.defaultPass(), messageRefID, conversationID));
		}
		log.info("Sent messages " + noOf);

		rest.deletePluginUser(user, domainCode);
		log.info("deleted plugin user" + user);
		return messIDs;
	}

	public JSONObject getUser(String domainCode, String role, boolean active, boolean deleted, boolean forceNew) throws Exception {
		String username = Generator.randomAlphaNumeric(10);

		if (StringUtils.isEmpty(domainCode)) {
			domainCode = "default";
		}

		if (!forceNew) {
			log.info("trying to find existing user with desired config");
			JSONArray users = rest.getUsers(domainCode);
			for (int i = 0; i < users.length(); i++) {
				JSONObject user = users.getJSONObject(i);
				if (StringUtils.equalsIgnoreCase(user.getString("userName"), "super")
						|| StringUtils.equalsIgnoreCase(user.getString("userName"), "admin")
						|| StringUtils.equalsIgnoreCase(user.getString("userName"), "user")
				) {
					log.info("skipping default users");
					continue;
				}

				if (StringUtils.equalsIgnoreCase(user.getString("domain"), domainCode)
						&& StringUtils.equalsIgnoreCase(user.getString("roles"), role)
						&& user.getBoolean("active") == active
						&& user.getBoolean("deleted") == deleted) {
					log.info("found user " + user.getString("userName"));
					return user;
				}
			}
		}

		rest.createUser(username, role, data.defaultPass(), domainCode);
		log.info("created user " + username);

		if (!active) {
			rest.blockUser(username, domainCode);
			log.info("deactivated user " + username);
		}
		if (deleted) {
			rest.deleteUser(username, domainCode);
			log.info("deleted user " + username);
		}

		JSONArray users = rest.getUsers(domainCode);
		log.info("searching for user in the system");
		for (int i = 0; i < users.length(); i++) {
			JSONObject user = users.getJSONObject(i);
			if (StringUtils.equalsIgnoreCase(user.getString("userName"), username)) {
				log.info("user found and returned");
				return user;
			}
		}
		log.info("user not found .. returning null");
		return null;
	}

	public String getUsername(String domainCode, String role, boolean active, boolean deleted, boolean forceNew) throws Exception {
		return getUser(domainCode, role, active, deleted, forceNew).getString("userName");
	}

	public JSONObject getPluginUser(String domainCode, String role, boolean active, boolean forceNew) throws Exception {
		String username = Generator.randomAlphaNumeric(10);

		if (StringUtils.isEmpty(domainCode)) {
			domainCode = "default";
		}

		if (!forceNew) {
			log.info("trying to find existing user with desired config");
			JSONArray users = rest.getPluginUsers(domainCode, "BASIC");
			for (int i = 0; i < users.length(); i++) {
				JSONObject user = users.getJSONObject(i);
				if (StringUtils.equalsIgnoreCase(user.getString("userName"), "super")
						|| StringUtils.equalsIgnoreCase(user.getString("userName"), "admin")
						|| StringUtils.equalsIgnoreCase(user.getString("userName"), "user")
				) {
					log.info("skipping default users");
					continue;
				}

				if (!StringUtils.equalsIgnoreCase(user.getString("userName"), "null")
						&& StringUtils.equalsIgnoreCase(user.getString("authRoles"), role)
						&& user.getBoolean("active") == active) {
					log.info("found user " + user.getString("userName"));
					return user;
				}
			}
		}

		rest.createPluginUser(username, role, data.defaultPass(), domainCode);
		log.info("created user " + username);

		if (!active) {
			rest.blockUser(username, domainCode);
			log.info("deactivated user " + username);
		}

		JSONArray users = rest.getPluginUsers(domainCode, "BASIC");
		log.info("searching for user in the system");
		for (int i = 0; i < users.length(); i++) {
			JSONObject user = users.getJSONObject(i);
			if (StringUtils.equalsIgnoreCase(user.getString("userName"), username)) {
				log.info("user found and returned");
				return user;
			}
		}
		log.info("user not found .. returning null");
		return null;
	}

	public String getPluginUsername(String domainCode, String role, boolean active, boolean forceNew) throws Exception {
		return getPluginUser(domainCode, role, active, forceNew).getString("userName");
	}


	public String getNonDefaultDomain() throws Exception {
		log.info("getting domains");
		List<String> domains = rest.getDomainNames();
		String domain1 = "";
		for (String domain : domains) {
			if (!StringUtils.equalsIgnoreCase(domain, "Default")) {
				domain1 = domain;
				break;
			}
		}
		return domain1;
	}

	public void generateTestData() throws Exception {

		log.info("GENERATING TEST DATA");

		String pass = data.defaultPass();

		int noOfMess = rest.getListOfMessages(null).length();
		if (noOfMess < 15) {
			rest.uploadPMode("pmodes/pmode-dataSetupBlue.xml", null);
			String pluginUsername = getPluginUser(null, DRoles.ADMIN, true, false).getString("userName");
			for (int i = noOfMess; i < 15; i++) {
				messageSender.sendMessage(pluginUsername, pass, Generator.randomAlphaNumeric(20), Generator.randomAlphaNumeric(20));
			}
		}

		JSONArray messageFilters = rest.getMessageFilters(null);
		for (int i = 0; i < messageFilters.length(); i++) {
			JSONObject obj = messageFilters.getJSONObject(i);
			if (!obj.getBoolean("persisted")) {
				rest.saveMessageFilters(messageFilters, null);
				break;
			}
		}
		log.info("DONE GENERATING TEST DATA");
	}


	protected <T extends FilterArea> void basicFilterPresence(SoftAssert soft, T filtersArea, JSONArray filtersDescription) throws Exception {

		log.info("checking basic filter presence");
		Field[] fields = filtersArea.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (!field.getType().toString().contains("WebElement")) {
				log.info(String.format("Skipping filed %s because it is not of type WebElement.", field.getName()));
				continue;
			}

			for (int i = 0; i < filtersDescription.length(); i++) {
				JSONObject currentNode = filtersDescription.getJSONObject(i);

				if (StringUtils.equalsIgnoreCase(currentNode.getString("name"), field.getName())) {

					log.info(String.format("Evaluating filter with description %s", currentNode.toString()));

					WebElement element = (WebElement) field.get(filtersArea);
					DObject object = new DObject(filtersArea.getDriver(), element);

					soft.assertEquals(object.isPresent(), currentNode.getBoolean("isDefault"),
							String.format("Filter %s isChangePassLnkPresent = %s as expected", field.getName(), currentNode.getBoolean("isDefault")));
					if (currentNode.getBoolean("isDefault")) {
						log.info(object.getAttribute("placeholder"));
						soft.assertEquals(object.getAttribute("placeholder"), currentNode.getString("placeholder"), "Placeholder text is correct - " + currentNode.getString("placeholder"));
					}
					continue;
				}
			}

		}
	}

	protected <T extends FilterArea> void advancedFilterPresence(SoftAssert soft, T filtersArea, JSONArray filtersDescription) throws Exception {

		log.info("checking advanced filter presence");

		Field[] fields = filtersArea.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (!field.getType().toString().contains("WebElement")) {
				log.info(String.format("Skipping filed %s because it is not of type WebElement.", field.getName()));
				continue;
			}

			for (int i = 0; i < filtersDescription.length(); i++) {
				JSONObject currentNode = filtersDescription.getJSONObject(i);
				if (StringUtils.equalsIgnoreCase(currentNode.getString("name"), field.getName())) {

					log.info(String.format("Evaluating filter with description %s", currentNode.toString()));

					WebElement element = (WebElement) field.get(filtersArea);
					DObject object = new DObject(filtersArea.getDriver(), element);

					soft.assertEquals(object.isPresent(), true,
							String.format("Filter %s as expected", field.getName()));

					String expected = currentNode.getString("placeholder");
					String actual = object.getAttribute("placeholder");

					if (StringUtils.isEmpty(expected) && StringUtils.isEmpty(actual)) {
						continue;
					}

					log.debug("placeholder: " + actual);
					soft.assertEquals(actual, expected, "Placeholder text is correct - " + expected);
				}
			}
		}
	}

	public <T extends DGrid> void testDefaultColumnPresence(SoftAssert soft, T grid, JSONArray gridDesc) throws Exception {
		log.info("Asserting grid default state");
		List<String> columns = new ArrayList<>();
		List<String> visibleColumns = grid.getColumnNames();

		for (int i = 0; i < gridDesc.length(); i++) {
			JSONObject colDesc = gridDesc.getJSONObject(i);
			if (colDesc.getBoolean("visibleByDefault")) {
				columns.add(colDesc.getString("name"));
			}
		}

		for (String column : columns) {
			soft.assertTrue(visibleColumns.contains(column), String.format("Column %s is found to be visible", column));
		}
	}

	public <T extends DomibusPage> void testButtonPresence(SoftAssert soft, T page, JSONArray buttons) throws Exception {
		log.info("Asserting button default state");

		Field[] fields = page.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (!field.getType().toString().contains("WebElement")) {
				log.info(String.format("Skipping filed %s because it is not of type WebElement.", field.getName()));
				continue;
			}

			for (int i = 0; i < buttons.length(); i++) {
				JSONObject curButton = buttons.getJSONObject(i);
				if (StringUtils.equalsIgnoreCase(curButton.getString("name"), field.getName())) {
					log.info(String.format("Evaluating button with description %s", curButton.toString()));

					WebElement element = (WebElement) field.get(page);
					DButton dButton = new DButton(driver, element);

					soft.assertEquals(dButton.isVisible(), curButton.getBoolean("visibleByDefault"), String.format("Button %s visibility is as described", curButton.getString("label")));
					soft.assertEquals(dButton.isEnabled(), curButton.getBoolean("enabledByDefault"), String.format("Button %s is enabled/disabled as described", curButton.getString("label")));
					soft.assertEquals(dButton.getText(), curButton.getString("label"), String.format("Button %s has expected label", curButton.getString("label")));
				}
			}

		}
	}

	protected <T extends DGrid> void testColumnControlsAvailableOptions(SoftAssert soft, T grid, JSONArray columns) throws Exception {
		log.info("checking column controls and available options");
		List<String> controlOptions = new ArrayList<>(grid.getGridCtrl().getAllCheckboxStatuses().keySet());
		for (int i = 0; i < columns.length(); i++) {
			String currentColumn = columns.getJSONObject(i).getString("name");
			log.info("Check option for " + currentColumn);
			soft.assertTrue(controlOptions.contains(currentColumn),
					String.format("Column %s present in the list of options in column controls", currentColumn));
		}
	}




}
