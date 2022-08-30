package ai.devtools.appium;

import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Point;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.URL;

public class BasicCrawlEspresso {

    @Test public void test() {
        try {
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability(MobileCapabilityType.APP, new File("/Users/etienne/apks/app-release.apk").getAbsolutePath());
            capabilities.setCapability("allowTestPackages", true);
            capabilities.setCapability("appWaitForLaunch", false);
            capabilities.setCapability("newCommandTimeout", 0);
            capabilities.setCapability("automationName", "Espresso");
            capabilities.setCapability("platformName", "Android");
            capabilities.setCapability("platformVersion", "9");
            capabilities.setCapability("appium:remoteAdbHost", "0.0.0.0");
            capabilities.setCapability("appium:host", "0.0.0.0");
            capabilities.setCapability("appium:useKeystore", true);
            capabilities.setCapability("appium:keystorePath", "/Users/etienne/Documents/old_format_keystore.keystore");
            capabilities.setCapability("appium:keystorePassword", "testtesttest");
            capabilities.setCapability("appium:keyAlias", "key0");
            capabilities.setCapability("appium:keyPassword", "testtesttest");
            capabilities.setCapability("forceEspressoRebuild", true);
            capabilities.setCapability("udid", "emulator-5554");
            capabilities.setCapability("noReset", false);
            capabilities.setCapability("espressoBuildConfig", "{ \"additionalAppDependencies\": [ \"androidx.lifecycle:lifecycle-extensions:2.2.0\" ] }");

            System.out.println("Starting test");

            AndroidDriver<MobileElement> androidDriver = new AndroidDriver<MobileElement>(new URL("http://0.0.0.0:4723/wd/hub"), capabilities);
            SmartDriver<MobileElement> smartDriver = new SmartDriver<MobileElement>(androidDriver, "4bdfb52c9b77fc14ef50dcfb");

            MobileElement element = smartDriver.findByAI("appium_debug_el_3");
            System.out.println("app:id/buttonresource-id " + element.getAttribute("resource-id"));

            MobileElement x = smartDriver.findElement(MobileBy.id("com.:id/button"));
            x.click();
            Thread.sleep(2000);

            element = smartDriver.findByAI("appium_debug_el3");
            element.click();

            element = smartDriver.findByAI("appium_debug_el4");
            element.click();
            element.sendKeys("test");
            Thread.sleep(5000);
            smartDriver.quit();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
