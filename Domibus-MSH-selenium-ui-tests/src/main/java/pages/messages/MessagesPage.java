package pages.messages;

import ddsl.dcomponents.DomibusPage;
import ddsl.dcomponents.grid.DGrid;
import ddsl.dcomponents.grid.GridControls;
import ddsl.dobjects.DButton;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.testng.asserts.SoftAssert;

import java.util.HashMap;
import java.util.List;

/**
 * @author Catalin Comanici
 * @since 4.1
 */
public class MessagesPage extends DomibusPage {


    public MessagesPage(WebDriver driver) {
        super(driver);
        log.debug("Messages page init");
        PageFactory.initElements(new AjaxElementLocatorFactory(driver, data.getTIMEOUT()), this);

    }

    @FindBy(id = "messageLogTable")
    public WebElement gridContainer;

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

    public MessageFilters getFilters() {
        return new MessageFilters(driver);
    }

    public GridControls gridControls() {
        return new GridControls(driver);
    }

    public boolean isLoaded() {
        return (getDownloadButton().isPresent()
                && getResendButton().isPresent()
                && null != grid()
                && null != getFilters());
    }

    public String getCssofRowSpecificActionIcon(int rowNumber, String iconName) {
        if (iconName.equals("Download")) {
            return "#downloadButtonRow" + rowNumber + "_id";
        }
        if (iconName.equals("Resend")) {
            return "#resendButtonRow" + rowNumber + "_id";
        }
        return "";
    }

    public Boolean getActionIconStatus(int rowNumber, String iconName) {
        WebElement iconElement = driver.findElement(By.cssSelector(getCssofRowSpecificActionIcon(rowNumber, iconName)));
        wait.forElementToBeVisible(iconElement);
        return iconElement.isEnabled();
    }

    public void compareGridData(SoftAssert soft, String columnName) throws Exception {
        int firstDomainGridRowCount = grid().getPagination().getTotalItems();
        List<String> msgIds = grid().getValuesOnColumn(columnName);

        getDomainSelector().selectOptionByIndex(1);
        log.info("Domain name is " + getDomainSelector().selectOptionByIndex(1));

        int secondDomainGridRowCount = grid().getPagination().getTotalItems();
        List<String> msgIdsSecDomain = grid().getValuesOnColumn(columnName);
        int secDomainActivePg=grid().getPagination().getActivePage();

        log.info("Active page for new Domain is " + secDomainActivePg);
        soft.assertTrue(secDomainActivePg==1,"After navigation user is at Pg 1");


        if (firstDomainGridRowCount == 0 && secondDomainGridRowCount == 0) {
            log.info("No need of comparison as both domain have empty grid");
        } else if (firstDomainGridRowCount == secondDomainGridRowCount) {
            log.info("Check data difference for both grid");
            for (int i = 0; i < msgIds.size(); i++) {
                if (msgIds.get(i).equals(msgIdsSecDomain.get(i))) {

                    log.info("Both domains have record with same column value for column : " +columnName+" row : " + i);
                    break;
                }
            }
            log.info("All data are different");


        } else {
            log.info("No need to check data as both have diff grid count");

        }
    }
}
