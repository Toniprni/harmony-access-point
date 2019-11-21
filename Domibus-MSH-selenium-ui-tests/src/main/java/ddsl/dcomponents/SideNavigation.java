package ddsl.dcomponents;

import ddsl.dobjects.DButton;
import ddsl.dobjects.DLink;
import ddsl.enums.PAGES;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Catalin Comanici

 * @version 4.1
 */


public class SideNavigation extends DComponent {

	public SideNavigation(WebDriver driver) {
		super(driver);
		PageFactory.initElements(new AjaxElementLocatorFactory(driver, 1), this);
	}

	@FindBy(tagName = "mat-sidenav")
	private WebElement sideBar;

	private WebElement topLogo;
	private WebElement topLogoText;

	@FindBy(id = "messages_id")
	private WebElement messagesLnk;

	@FindBy(id = "messagefilter_id")
	private WebElement messagefilterLnk;

	@FindBy(id = "errorlog_id")
	private WebElement errorlogLnk;

	//	----------------------------------------------------
	@FindBy(css = "div.mat-expansion-panel-content > div > div > button:nth-child(1)")
	private WebElement pmodeCurrentLnk;

	@FindBy(css = "div.mat-expansion-panel-content > div > div > button:nth-child(2)")
	private WebElement pmodeArchiveLnk;

	@FindBy(css = "div.mat-expansion-panel-content > div > div > button:nth-child(3)")
	private WebElement pmodePartiesLnk;

	@FindBy(css = "mat-sidenav mat-expansion-panel mat-expansion-panel-header")
	private WebElement pmodeExpandLnk;
	//	----------------------------------------------------


	@FindBy(id = "jmsmonitoring_id")
	private WebElement jmsmonitoringLnk;

	@FindBy(id = "truststore_id")
	private WebElement truststoreLnk;

	@FindBy(id = "user_id")
	private WebElement userLnk;

	@FindBy(css = "#plugin_user_id")
	private WebElement pluginUsersLnk;

	@FindBy(css = "#audit_id")
	private WebElement auditLnk;

	@FindBy(css = "#alerts_id")
	private WebElement alertsLnk;

	@FindBy(css = "#testservice_id")
	private WebElement testServiceLnk;

	private boolean isPmodeSectionExpanded() {
		try {
			return new DButton(driver, pmodeExpandLnk).getAttribute("class").contains("mat-expanded");
		} catch (Exception e) {
		}
		return false;
	}

	private void expandPmodeSection() {
		if (isPmodeSectionExpanded()) return;
		try {
			new DButton(driver, pmodeExpandLnk).click();
		} catch (Exception e) {
			log.warn("Could not expand pmode: ", e);
		}
	}

	public DLink getPageLnk(PAGES page){

		wait.forElementToHaveText(sideBar);

		log.debug("Get link to " + page.name());
		switch (page) {
			case MESSAGES:
				return new DLink(driver, messagesLnk);
			case MESSAGE_FILTER:
				return new DLink(driver, messagefilterLnk);
			case ERROR_LOG:
				return new DLink(driver, errorlogLnk);
			case PMODE_CURRENT:
				expandPmodeSection();
				return new DLink(driver, pmodeCurrentLnk);
			case PMODE_ARCHIVE:
				expandPmodeSection();
				return new DLink(driver, pmodeArchiveLnk);
			case PMODE_PARTIES:
				expandPmodeSection();
				return new DLink(driver, pmodePartiesLnk);
			case JMS_MONITORING:
				return new DLink(driver, jmsmonitoringLnk);
			case TRUSTSTORE:
				return new DLink(driver, truststoreLnk);
			case USERS:
				return new DLink(driver, userLnk);
			case PLUGIN_USERS:
				return new DLink(driver, pluginUsersLnk);
			case AUDIT:
				return new DLink(driver, auditLnk);
			case ALERTS:
				return new DLink(driver, alertsLnk);
			case TEST_SERVICE:
				return new DLink(driver, testServiceLnk);
		}
		return null;
	}

	public List<String> availableOptions() throws Exception {

		wait.forElementToBeClickable(messagesLnk);
		List<String> links = new ArrayList<>();

		for (PAGES domibus_page : PAGES.values()) {
			DLink link = getPageLnk(domibus_page);
			try {
				if (link.isEnabled()) {
					links.add(link.getLinkText());
				}
			} catch (Exception e) {
			}
		}

		return links;

	}

	public void goToPage(PAGES page) throws Exception {
		log.info("Navigating to " + page.name());
		getPageLnk(page).click();
		log.debug("Navigated to " + page.name());
	}

	public boolean isUserState() throws Exception {
		return (getPageLnk(PAGES.MESSAGES).isPresent()
				&& getPageLnk(PAGES.ERROR_LOG).isPresent()

				&& !getPageLnk(PAGES.MESSAGE_FILTER).isPresent()
				&& !getPageLnk(PAGES.PMODE_CURRENT).isPresent()
				&& !getPageLnk(PAGES.PMODE_ARCHIVE).isPresent()
				&& !getPageLnk(PAGES.PMODE_PARTIES).isPresent()
				&& !getPageLnk(PAGES.JMS_MONITORING).isPresent()
				&& !getPageLnk(PAGES.TRUSTSTORE).isPresent()
				&& !getPageLnk(PAGES.USERS).isPresent()
				&& !getPageLnk(PAGES.PLUGIN_USERS).isPresent()
				&& !getPageLnk(PAGES.AUDIT).isPresent()
				&& !getPageLnk(PAGES.ALERTS).isPresent()
				&& !getPageLnk(PAGES.TEST_SERVICE).isPresent()
		);
	}

	public boolean isAdminState() throws Exception {
		return (getPageLnk(PAGES.MESSAGES).isPresent()
				&& getPageLnk(PAGES.ERROR_LOG).isPresent()
				&& getPageLnk(PAGES.MESSAGE_FILTER).isPresent()
				&& getPageLnk(PAGES.PMODE_CURRENT).isPresent()
				&& getPageLnk(PAGES.PMODE_ARCHIVE).isPresent()
				&& getPageLnk(PAGES.PMODE_PARTIES).isPresent()
				&& getPageLnk(PAGES.JMS_MONITORING).isPresent()
				&& getPageLnk(PAGES.TRUSTSTORE).isPresent()
				&& getPageLnk(PAGES.USERS).isPresent()
				&& getPageLnk(PAGES.PLUGIN_USERS).isPresent()
				&& getPageLnk(PAGES.AUDIT).isPresent()
				&& getPageLnk(PAGES.ALERTS).isPresent()
				&& getPageLnk(PAGES.TEST_SERVICE).isPresent()
		);
	}


}
