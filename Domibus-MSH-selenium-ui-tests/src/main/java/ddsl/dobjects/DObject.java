package ddsl.dobjects;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


public class DObject {

	protected WebDriver driver;
	protected DWait wait;

	//	protected Logger log = LoggerFactory.getLogger(this.getClass());
	public WebElement element;

	public DObject(WebDriver driver, WebElement element) {
		wait = new DWait(driver);
		this.driver = driver;
		this.element = element;
	}

	public void clickVoidSpace() {
		try {
			wait.forXMillis(300);
			((JavascriptExecutor) driver).executeScript("document.querySelector('[class*=\"overlay-backdrop\"]').click()");
			wait.forXMillis(300);
		} catch (Exception e) {
		}
		wait.forXMillis(300);
	}

	public boolean isPresent() {
		try {
			wait.forElementToBe(element);
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", element);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public boolean isEnabled() throws Exception {
		if (isPresent()) {
			wait.forElementToBeEnabled(element);
			return element.isEnabled();
		}
		throw new Exception("Element not present");
	}

	public String getText() throws Exception {
		if (!isPresent()) {
			throw new Exception("Element not present");
		}
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView();", element);
		String text = ((JavascriptExecutor) driver).executeScript("return arguments[0].textContent;", element).toString();
		return text.trim();
	}

	public void click() throws Exception {
		if (isEnabled()) {
			wait.forElementToBeClickable(element).click();
			wait.forXMillis(100);
		} else {
			throw new Exception("Not enabled");
		}
	}

	public String getAttribute(String attributeName) throws Exception {
		if (isPresent()) {
			return element.getAttribute(attributeName).trim();
		}
		throw new Exception("Element not present");
	}
}
