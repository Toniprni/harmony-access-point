package pages.users;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dcomponents.popups.Dialog;
import ddsl.dobjects.DButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;



/**
 * @author Catalin Comanici

 * @version 4.1
 */


public class UsersPage extends DomibusPage {
	public UsersPage(WebDriver driver) {
		super(driver);
		PageFactory.initElements(new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);
	}

	@FindBy(id = "userTable")
	private WebElement userTableContainer;

	@FindBy(id = "userCancelButton")
	private WebElement cancelBtn;

	@FindBy(id = "userSaveButton")
	private WebElement saveBtn;

	@FindBy(id = "userNewButton")
	private WebElement newBtn;

	@FindBy(id = "userEditButton")
	private WebElement editBtn;

	@FindBy(id = "userDeleteButton")
	private WebElement deleteBtn;

	public DGrid grid() {
		return new DGrid(driver, userTableContainer);
	}

	public UsersGrid getUsersGrid() {
		return new UsersGrid(driver, userTableContainer);
	}

	public DButton getCancelBtn() {
		return new DButton(driver, cancelBtn);
	}

	public DButton getSaveBtn() {
		return new DButton(driver, saveBtn);
	}

	public DButton getNewBtn() {
		return new DButton(driver, newBtn);
	}

	public DButton getEditBtn() {
		return new DButton(driver, editBtn);
	}

	public DButton getDeleteBtn() {
		return new DButton(driver, deleteBtn);
	}

	public boolean isLoaded() throws Exception {
		return (getCancelBtn().isPresent()
				&& getSaveBtn().isPresent()
				&& getNewBtn().isEnabled()
				&& getEditBtn().isPresent()
				&& getDeleteBtn().isPresent()
				&& grid().isPresent());
	}

	public void saveAndConfirm() throws Exception {
		log.info("saving");
		getSaveBtn().click();
		new Dialog(driver).confirm();
	}

	public void cancelAndConfirm() throws Exception {
		log.info("canceling");
		getCancelBtn().click();
		new Dialog(driver).confirm();
	}

	public void newUser(String user, String email, String role, String password, String confirmation) throws Exception {
		getNewBtn().click();
		UserModal modal = new UserModal(driver);
		modal.fillData(user, email, role, password, confirmation);
		modal.getOkBtn().click();
	}

	public void editUser(String user, String email, String role, String password, String confirmation) throws Exception {
		getEditBtn().click();

		UserModal modal = new UserModal(driver);
		modal.fillData("", email, role, password, confirmation);
		modal.getOkBtn().click();
	}
}
