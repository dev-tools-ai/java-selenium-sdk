package ai.devtools.appium;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.URL;

public class DefaultDemo {

    @Test public void test() {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(MobileCapabilityType.APP, new File("/Users/etienne/apks/todoist.apk").getAbsolutePath());
            capabilities.setCapability("allowTestPackages", true);
            capabilities.setCapability("appWaitForLaunch", false);
            capabilities.setCapability("newCommandTimeout", 0);

            System.out.println("Starting test");

            AndroidDriver<MobileElement> androidDriver = new AndroidDriver<MobileElement>(new URL("http://localhost:4723/wd/hub"), capabilities);
            SmartDriver<MobileElement> smartDriver = new SmartDriver<MobileElement>(androidDriver, "4bdfb52c9b77fc14ef50dcfb");
            Thread.sleep(5000);
            MobileElement ingest = smartDriver.findElement(By.xpath("com.todoist:id/btn_welcome_email"));
            ingest.click();

            //MobileElement element = smartDriver.findByAI("todoist_login");
            //element.click();
            Thread.sleep(4000);

            MobileElement element = smartDriver.findByAI("todoist_username");
            element.click();
            element.sendKeys("etienne@dev-tools.ai");

            smartDriver.quit();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
