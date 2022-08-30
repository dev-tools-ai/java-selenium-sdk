package ai.devtools.appium;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.URL;

public class BasicCrawlIOS {

    @Test public void test() {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(MobileCapabilityType.APP, new File("/Users/etienne/apks/testAiSampleApp.app").getAbsolutePath());
            capabilities.setCapability("allowTestPackages", true);
            capabilities.setCapability("appWaitForLaunch", false);
            capabilities.setCapability("newCommandTimeout", 0);
            capabilities.setCapability("automationName", "XCUITest");
            capabilities.setCapability("platformName", "iOS");
            capabilities.setCapability("platformVersion", "14.4");
            capabilities.setCapability("deviceName", "iPhone 12 Pro Max");
            //capabilities.setCapability("app", "Domo Debug");
            //capabilities.setCapability("forceEspressoRebuild", true);

            System.out.println("Starting test");
            IOSDriver<MobileElement> androidDriver = new IOSDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
            SmartDriver<MobileElement> smartDriver = new SmartDriver<MobileElement>(androidDriver, "4bdfb52c9b77fc14ef50dcfb");


            MobileElement element = smartDriver.findByAI("appium_ios_element_1");
            element.click();
            element.sendKeys("hello");
            element = smartDriver.findByAI("appium_ios_element_2");
            element = smartDriver.findByAI("appium_ios_element_3");

            smartDriver.quit();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
