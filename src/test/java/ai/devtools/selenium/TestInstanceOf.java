package ai.devtools.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.Mockito.verify;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import org.mockito.Mockito;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.mockito.Mockito.verify;

public class TestInstanceOf {

    @Test
    void seleniumTest() throws Throwable {
        /*
        This test shows how to open a new tab and close it with SmartDriver.
        Also shows that SmartDriver is instanceof WebDriver and SmartDriver.
         */
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless"); uncomment if you want headless
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("window-size=1280x1024");
        ChromeDriver chromeDriver = new ChromeDriver(options);

        String api_key = "abcd";
        HashMap<String, Object> config = new HashMap<String, Object>();
        config.put("testCaseName", "some_tc");
        SmartDriver baseDriver = new SmartDriver(chromeDriver, api_key, config);


        baseDriver.get("https://google.com");
        ((JavascriptExecutor) baseDriver).executeScript("window.open()");
        ArrayList<String> tabs = new ArrayList<String>(baseDriver.getWindowHandles());
        baseDriver.switchTo().window(tabs.get(1));
        baseDriver.get("https://bbc.co.uk");
        Thread.sleep(3000);
        baseDriver.close();

        boolean isInstanceOfSmartDriver = baseDriver instanceof SmartDriver;
        boolean isInstanceOfWebDriver = baseDriver instanceof WebDriver;
        assert (isInstanceOfSmartDriver);
        assert (isInstanceOfWebDriver);
    }
}

