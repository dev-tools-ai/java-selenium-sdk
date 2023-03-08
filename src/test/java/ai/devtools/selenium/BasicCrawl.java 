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
        options.addArguments("window-size=1600x1200");
        options.addArguments("--force-device-scale-factor=2.0");
        ChromeDriver chromeDriver = new ChromeDriver(options);
        try {
            String api_key = "<<get your api key at smartdriver.dev-tools.ai>>";
            HashMap<String, Object> config = new HashMap<String, Object>();
            config.put("testCaseName", "local_ci_test");
            config.put("classifyMaxRetries", 1);
            config.put("useFastJsChopper", true);
            SmartDriver baseDriver = new SmartDriver(chromeDriver, api_key, config);
            SmartDriver driver = Mockito.spy(baseDriver);
            driver.get("http://0.0.0.0:5005/index.html");

            Thread.sleep(2000);

            WebElement element = driver.findByAI("el_what_artists_notice", 0.99f);
            String html = element.getAttribute("innerHTML");
            String out = outContent.toString();
            System.out.println(out);
            assert (
                    out.contains("Successful classification of el_what_artists_notice") ||
                    out.contains("Cache hit for el_what_artists_notice") ||
	            out.contains("Screenshot exists. Found element on screenshot for el_what_artists_notice"));
            element.click();
            Thread.sleep(2000);

            WebElement paintersHeader = driver.findElementByXPath("//h1[contains(text(),'Painters')]");
            verify(driver, Mockito.times(1)).updateElement(Mockito.any(), Mockito.anyString(), Mockito.eq("element_name_by_locator_By_xpath:_//h1[contains(text(),'Painters')]"), Mockito.anyBoolean());
            assert (paintersHeader.isDisplayed());
            driver.get("http://localhost:5005/index.html");


            Thread.sleep(2000);

            WebElement clojureLink = driver.findElement(By.xpath("//a[contains(text(),'bad_selector')]"), 0.99f);
            verify(driver, Mockito.times(1)).classify(Mockito.eq("element_name_by_locator_By_xpath:_//a[contains(text(),'bad_selector')]"),
                    Mockito.eq(0.99f));
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
