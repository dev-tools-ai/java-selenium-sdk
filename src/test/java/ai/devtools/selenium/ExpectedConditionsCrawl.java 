package ai.devtools.selenium;

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
        options.addArguments("window-size=1600x1200");
        options.addArguments("--force-device-scale-factor=2.0");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        String api_key = "<<get your api key at smartdriver.dev-tools.ai>>";
        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("testCaseName", "local_ci_test");
        SmartDriver baseDriver = new SmartDriver(chromeDriver, api_key, config);
        PropertyConfigurator.configure("log4j.properties");
        SmartDriver driver = Mockito.spy(baseDriver);
        driver.get("http://localhost:5005/index.html");

        Thread.sleep(2000);

        WebDriverWait wait = new WebDriverWait(driver, 10);
        By why_change_dynamic = By.ai("el_why_change", 0.99f);
        wait.until(ExpectedConditions.presenceOfElementLocated(why_change_dynamic));

        WebElement element = driver.findElement(why_change_dynamic);
        element.click();
        Thread.sleep(2000);
        System.out.println("Should be on never gonna give you up page");
        // "Never gonna give you up" should be in page source
        assert driver.getPageSource().contains("Never gonna give you up");
        Thread.sleep(2000);


        driver.get("http://localhost:5005/index.html");

        wait = new WebDriverWait(driver, 10);
        org.openqa.selenium.By elem_dynamic = By.xpath("//a[text()='Antifragility']");
        wait.until(ExpectedConditions.presenceOfElementLocated(elem_dynamic));

        element = driver.findByAI("el_some_link");
        element.click();


        driver.quit();
    }
}
