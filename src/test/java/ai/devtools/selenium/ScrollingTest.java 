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
        options.addArguments("window-size=1600x1200");
        options.addArguments("--force-device-scale-factor=2.0");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        try {
            String api_key = "<<get your api key at smartdriver.dev-tools.ai>>";
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testCaseName", "local_ci_test");
            SmartDriver driver = new SmartDriver(chromeDriver, api_key, config);
            driver.get("http://localhost:5005/index.html");
            // scroll down
            driver.scrollPage(0);
            Thread.sleep(2000);

            // Find an element mid way through the page
            WebElement element = driver.findByAI("el_why_change", 0.95f);
            element.click();
            Thread.sleep(2000);
            assert(driver.getPageSource().contains("Never gonna give you up"));
        } finally {
            chromeDriver.quit();
        }
    }
}
