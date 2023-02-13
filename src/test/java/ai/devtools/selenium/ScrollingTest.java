package ai.devtools.selenium;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import org.mockito.Mockito;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.mockito.Mockito.verify;

public class ScrollingTest  {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Test void testScrollStopa() throws Throwable
    {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("window-size=1280x1024");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        try {
            String api_key = "4bdfb52c9b77fc14ef50dcfb";
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testCaseName", "stopa_navigation");
            SmartDriver driver = new SmartDriver(chromeDriver, api_key, config);
            driver.get("https://stopa.io");
            // scroll down
            driver.scrollPage(100000);
            Thread.sleep(2000);

            // Find an element mid way through the page
            WebElement element = driver.findByAI("element_name_by_locator_By_partialLinkText:_How_to_Pick_a_Language");
            element.click();
            Thread.sleep(2000);
            assert(driver.getPageSource().contains("Start with Constraints"));
        } finally {
            chromeDriver.quit();
        }
    }
}
