package pages.messages;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dobjects.DButton;
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
public class MessagesPage extends DomibusPage {


	public MessagesPage(WebDriver driver) {
		super(driver);
		log.info("Messages page init");
		PageFactory.initElements( new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);

	}

	@FindBy(id = "messageLogTable")
	public  WebElement gridContainer;

	@FindBy(id = "downloadbutton_id")
	public WebElement downloadBtn;

	@FindBy(id = "resendbutton_id")
	public WebElement resendBtn;


	public DGrid grid() {
		return new DGrid(driver, gridContainer);
	}

	public DButton getDownloadButton() {
		return new DButton(driver, downloadBtn);
	}

	public DButton getResendButton() {
		return new DButton(driver, resendBtn);
	}

	public MessageFilters getFilters(){ return new MessageFilters(driver);}

	public boolean isLoaded(){
		return (getDownloadButton().isPresent()
		&& getResendButton().isPresent()
		&& null != grid()
		&& null != getFilters());
	}



}
