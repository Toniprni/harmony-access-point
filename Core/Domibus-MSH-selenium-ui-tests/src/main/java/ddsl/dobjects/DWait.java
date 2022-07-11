package ddsl.dobjects;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.DFileUtils;
import utils.TestRunData;

import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;


/**
 * @author Catalin Comanici
 * @version 4.1
 */


public class DWait {

	public final WebDriverWait defaultWait;
	public final WebDriverWait longWait;
	public final WebDriverWait shortWait;
	private TestRunData data = new TestRunData();

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private WebDriver driver;

	public DWait(WebDriver driver) {
		this.defaultWait = new WebDriverWait(driver, data.getTIMEOUT());
		this.longWait = new WebDriverWait(driver, data.getLongWait());
		this.shortWait = new WebDriverWait(driver, 1);
		this.driver = driver;
	}

	public void forXMillis(Integer millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			log.error("EXCEPTION: ", e);
		}
	}

	public WebElement forElementToBeClickable(WebElement element) {
		return defaultWait.until(ExpectedConditions.elementToBeClickable(element));
	}

	public WebElement forElementToBeVisible(WebElement element) {
		return defaultWait.until(ExpectedConditions.visibilityOf(element));
	}

	public WebElement forElementToBeVisible(WebElement element, boolean waitLonger) {
		if (waitLonger) {
			return longWait.until(ExpectedConditions.visibilityOf(element));
		}
		return defaultWait.until(ExpectedConditions.visibilityOf(element));
	}

	public void forElementToBeEnabled(WebElement element) {
		int maxTimeout = data.getTIMEOUT() * 1000;
		int waitedSoFar = 0;

		while ((null != element.getAttribute("disabled")) && (waitedSoFar < maxTimeout)) {
			waitedSoFar += 300;
			forXMillis(300);
		}
	}

	public void forElementToBeDisabled(WebElement element) {
		int maxTimeout = data.getTIMEOUT() * 1000;
		int waitedSoFar = 0;

		while ((null == element.getAttribute("disabled")) && (waitedSoFar < maxTimeout)) {
			waitedSoFar += 300;
			forXMillis(300);
		}
	}

	public void forAttributeNotEmpty(WebElement element, String attributeName) {
		defaultWait.until(ExpectedConditions.attributeToBeNotEmpty(element, attributeName));
	}

	public void forElementToBeGone(WebElement element) {
		forXMillis(500);
	}

	public void forElementToBeGone(By selector) {

		log.debug("Waiting for element to be gone: " + selector.toString());
		int c = 0;

		while (c < 50) {
			try {
				driver.findElement(selector);
			} catch (Exception e) {
				break;
			}
			forXMillis(100);
			c++;
		}

	}

	public void forElementToBe(WebElement element) {

		defaultWait.until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				return element.getLocation() != null;
			}
		});

	}

	public void shortWaitForElementToBe(WebElement element) {

		shortWait.until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				return element.getLocation() != null;
			}
		});

	}

	public void forAttributeToContain(WebElement element, String attributeName, String value) {
		defaultWait.until(ExpectedConditions.attributeContains(element, attributeName, value));
	}

	public void forAttributeToNOTContain(WebElement element, String attributeName, String value) {
		defaultWait.until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				return !element.getAttribute(attributeName).contains(value);
			}
		});
	}

	public void forAttributeToContain(WebElement element, String attributeName, String value, boolean waitLonger) {

		WebDriverWait myWait = defaultWait;
		if (waitLonger) {
			myWait = longWait;
		}
		myWait.until(ExpectedConditions.attributeContains(element, attributeName, value));
	}

	public void forElementToHaveText(WebElement element) {
		defaultWait.until(new ExpectedCondition<Boolean>() {
			@Override
			public Boolean apply(WebDriver driver) {
				return !element.getText().trim().isEmpty();
			}
		});
	}

	public void forElementToContainText(WebElement element, String text) {
//		defaultWait.until(ExpectedConditions.textToBePresentInElement(element, text));

		defaultWait.until((ExpectedCondition<Boolean>) driver -> containsIgnoreCase(element.getText(), text));

	}

	public void forFileToBeDownloaded(String filePath) throws Exception {
		log.info("waiting for file to appear");

		int count = 0;
		boolean fileDownloaded = false;
		while (count < 50 && !DFileUtils.isFileDownloaded(filePath)) {
			forXMillis(100);
			count++;
		}
	}


}
