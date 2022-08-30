package ai.devtools.appium;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebElement;

import java.io.File;
import java.net.URL;

public class BasicWebCrawl {

    @Test public void test() {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("allowTestPackages", true);
            capabilities.setCapability("appWaitForLaunch", false);
            capabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, "UIAutomator2");
            capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "Chrome");
            capabilities.setCapability("newCommandTimeout", 0);

            AndroidDriver<MobileElement> androidDriver = new AndroidDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
            SmartDriver<MobileElement> smartDriver = new SmartDriver<MobileElement>(androidDriver, "4bdfb52c9b77fc14ef50dcfb");

            smartDriver.get("https://stopa.io");

            Thread.sleep(2000);
            MobileElement element = smartDriver.findElement(By.partialLinkText("What Artists"));
            Rectangle rect = element.getRect();

            element = smartDriver.findByAI("link What Artists Notice");
            String html = element.getAttribute("innerHTML");
            element.click();
            Thread.sleep(5000);
            smartDriver.quit();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
