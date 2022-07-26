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

public class BasicCrawl  {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test void seleniumTest() throws Throwable
    {
        setUpStreams();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("window-size=1280x1024");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        try {
            String api_key = "<<get your api key at smartdriver.dev-tools.ai>>";
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testCaseName", "stopa_navigation");
            SmartDriver baseDriver = new SmartDriver(chromeDriver, api_key, config);
            SmartDriver driver = Mockito.spy(baseDriver);
            driver.get("https://stopa.io");

            Thread.sleep(2000);

            WebElement element = driver.findByAI("link What Artists Notice");
            String out = outContent.toString();
            assert (
                    out.contains("Successful classification of link What Artists Notice") ||
                    out.contains("Cache hit for link What Artists Notice") ||
	            out.contains("Screenshot exists. Found element on screenshot for link What Artists Notice"));
            element.click();
            Thread.sleep(2000);

            WebElement paintersHeader = driver.findElementByXPath("//h1[contains(text(),'Painters')]");
            verify(driver, Mockito.times(1)).updateElement(Mockito.any(), Mockito.anyString(), Mockito.eq("element_name_by_locator_By_xpath:_//h1[contains(text(),'Painters')]"), Mockito.anyBoolean());
            assert (paintersHeader.isDisplayed());
            driver.get("https://stopa.io");


            Thread.sleep(2000);

            WebElement clojureLink = driver.findElementByXPath("//a[contains(text(),'bad_selector')]");
            verify(driver, Mockito.times(1)).classify(Mockito.eq("element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')]"));
            assert (clojureLink.isDisplayed());
            out = outContent.toString();
            assert(out.contains("Element 'element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')]' was not found by Selenium, trying with Smartdriver...") &&
                    ( out.contains("Screenshot exists. Found element on screenshot for element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')]") ||
                            out.contains("Cache hit for element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')] ") ||
                            out.contains("Successful classification of element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')]") ));
            clojureLink.click();
            Thread.sleep(2000);

            WebElement someText = driver.findElementByXPath("//a[contains(text(),'guide to Bel')]");
            assert (someText.isDisplayed());

        } finally {
            String finalOout = outContent.toString();
            String finalErr = errContent.toString();
            restoreStreams();
            System.out.println(finalOout); // useful for debugging if test goes wrong
            System.err.println(finalErr); // same
            chromeDriver.quit();
        }
    }


    @Test void sampleTest() throws Throwable
    {
        setUpStreams();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("window-size=1280x1024");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        try {
            String api_key = "<<get your api key at smartdriver.dev-tools.ai>>";
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testCaseName", "stopa_navigation");
            SmartDriver driver = new SmartDriver(chromeDriver, api_key, config);
            driver.get("https://google.com");

            Thread.sleep(2000);

            WebElement searchBoxElement = driver.findElement(By.name("q"));
            searchBoxElement.sendKeys("hello world\n");
        } finally {
            String finalOout = outContent.toString();
            String finalErr = errContent.toString();
            restoreStreams();
            System.out.println(finalOout); // useful for debugging if test goes wrong
            System.err.println(finalErr); // same
            chromeDriver.quit();
        }
    }
}
