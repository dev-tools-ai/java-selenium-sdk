package ai.devtools.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import org.mockito.Mockito;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.mockito.Mockito.verify;

import org.apache.log4j.PropertyConfigurator;

public class ExpectedConditionsCrawl {

    @Test void seleniumTest() throws Throwable
    {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("window-size=1280x1024");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        String api_key = "4bdfb52c9b77fc14ef50dcfb";
        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("testCaseName", "stopa_navigation");
        SmartDriver baseDriver = new SmartDriver(chromeDriver, api_key, config);
        PropertyConfigurator.configure("log4j.properties");
        SmartDriver driver = Mockito.spy(baseDriver);
        driver.get("https://stopa.io");

        Thread.sleep(2000);

        WebDriverWait wait = new WebDriverWait(driver, 10);
        By elem_dynamic = By.xpath("//a[text()='What Gödel Discovered']");
        wait.until(ExpectedConditions.presenceOfElementLocated(elem_dynamic));

        WebElement element = driver.findByAI("some_link");
        element.click();
        driver.quit();
    }
}
