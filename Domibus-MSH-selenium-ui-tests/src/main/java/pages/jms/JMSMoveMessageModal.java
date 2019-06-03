package pages.jms;

import ddsl.dcomponents.Select;
import ddsl.dcomponents.popups.EditModal;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * @author Catalin Comanici
 * @description:
 * @since 4.1
 */
public class JMSMoveMessageModal extends EditModal {
	public JMSMoveMessageModal(WebDriver driver) {
		super(driver);
		PageFactory.initElements(new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);
	}

	@FindBy(id = "messageDialogResendButton")
	protected WebElement okBtn;

	@FindBy(id = "messageDialogCancelButton")
	protected WebElement cancelBtn;

	@FindBy(id = "jmsqueuedestination_id")
	protected WebElement queueSelect;

	public Select getQueueSelect() {
		return new Select(driver, queueSelect);
	}





}
