package ddsl.dcomponents;

import ddsl.dobjects.DObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import utils.TestRunData;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


@SuppressWarnings("SpellCheckingInspection")
public class AlertArea extends DComponent {

	public AlertArea(WebDriver driver) {
		super(driver);
		PageFactory.initElements(new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);
	}

	@FindBy(id = "alertmessage_id")
	public WebElement alertMessage;

	public String getAlertMessage() throws Exception {

		DObject alertObject = new DObject(driver, alertMessage);

		if (!alertObject.isPresent()) {
			log.info("No messages displayed.");
			return null;
		}

//		String messageTxt = alertObject.getText().replaceAll("[^a-zA-Z0-9\\[\\]_:/\\.\\\\ ]", "").trim();
		String messageTxt = alertObject.getText().replaceAll("[^a-zA-Z0-9\\[\\]_:/\\.\\\\' ]", "").trim();

		log.info("Getting alert message ...");
		return messageTxt.trim();
	}

	public boolean isError() throws Exception {

		DObject alertObject = new DObject(driver, alertMessage);

		if (alertObject.isPresent()) {
			return (alertObject.getAttribute("class").contains("error"));
		}
		throw new Exception("Alert message not present");
	}


}
