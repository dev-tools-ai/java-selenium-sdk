package ai.devtools.appium;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import ai.devtools.utils.CollectionUtils;
import ai.devtools.utils.JsonUtils;
import ai.devtools.utils.NetUtils;
import ai.devtools.utils.Utils;
import com.google.gson.JsonNull;
import io.appium.java_client.ios.IOSDriver;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.interactions.Keyboard;
import org.openqa.selenium.interactions.Mouse;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.json.Json;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.slf4j.helpers.MessageFormatter;

import com.google.gson.JsonObject;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

/**
 * The {@code SmartDriver} class is a wrapper around a {@code RemoteWebDriver} that uses the results of the dev-tools.ai classifier for improved robustness, finding elements visually and avoiding broken selectors.
 */
@SuppressWarnings("deprecation")
public class SmartDriver<T extends MobileElement> {
    /**
     * The current version of the SDK
     */
    private static String SDK_VERSION = "appium-0.1.16";

    private boolean isMobileWeb = false;
    public boolean isIOS;
    public boolean isEspresso;

    /**
     * The logger for this class
     */
    private Logger log = Logger.getLogger(SmartDriver.class);

    /**
     * The client to use for making http requests
     */
    private OkHttpClient client;

    /**
     * The driver used by the user that we're wrapping.
     */
    public AppiumDriver<T> driver;

    /**
     * The user's Smartdriver API key
     */
    private String apiKey;


    /**
     * The base URL of the target server (e.g. {@code https://smartdriver.dev-tools.ai})
     */
    private HttpUrl serverURL;

    private String prodUrl = "https://smartdriver.dev-tools.ai";

    /**
     * The test case name. Used in live/interactive mode.
     */
    private String testCaseName;

    /**
     * The UUID of the last screenshot in live/interactive mode.
     */
    private String lastTestCaseScreenshotUUID;

    /**
     * The screen density multiplier
     */
    public double multiplier;
    private Dimension windowSize;
    private Dimension imSize;
    private Boolean useClassifierDuringCreation;
    private Boolean testCaseCreationMode;

    private String refScreenshotUUID;
    private float pageOffset;
    private float previousPageOffset;

    private String automationName;

    /**
     * Constructor, creates a new SmartDriver.
     *
     * @param driver The {@code RemoteWebDriver} to wrap
     * @param apiKey Your API key, acquired from <a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>.
     * @param initializationDict The configuration options for the driver.
     * @throws IOException If there was an initialization error.
     */
    public SmartDriver(AppiumDriver<T> driver, String apiKey, Map<String, Object> initializationDict) throws IOException
    {
        this.driver = driver;
        this.apiKey = apiKey;
        log.setLevel(org.apache.log4j.Level.INFO);
        BasicConfigurator.configure();

        isIOS = driver instanceof IOSDriver;
        Object automationNameObject = driver.getCapabilities().getCapability("automationName");
        Object platformNameObject = driver.getCapabilities().getCapability("platformName");
        String platformName = platformNameObject == null ? "": platformNameObject.toString();
        String automationName = automationNameObject == null ? "": automationNameObject.toString();

        if (automationName == "" && platformName == "Android") {
            automationName = "UiAutomator2";
        }

        isEspresso = automationName.equals("Espresso");
        //isMobileWeb = T instanceof RemoteWebElement;

        this.testCaseName = (String) initializationDict.get("testCaseName");
        this.useClassifierDuringCreation = true; // Default to running it because it's easier for customers
        if (initializationDict.get("useClassifierDuringCreation") != null) {
            this.useClassifierDuringCreation = (Boolean) initializationDict.get("useClassifierDuringCreation");
        };
        this.testCaseCreationMode = Utils.StrToBool(System.getenv("DEVTOOLSAI_INTERACTIVE"));

        if (testCaseName == null)
        {
            StackTraceElement[] sl = Thread.currentThread().getStackTrace();
            if (sl.length > 0)
            {
                StackTraceElement bottom = sl[sl.length - 1];
                this.testCaseName = String.format("%s.%s", bottom.getClassName(), bottom.getMethodName());

                log.info("No test case name was specified, defaulting to " + this.testCaseName);
            }
            else
                this.testCaseName = "My first test case";
        }

        String baseUrl = (String) initializationDict.get("serverURL");
        this.serverURL = HttpUrl.parse(baseUrl != null ? baseUrl : Objects.requireNonNullElse(System.getenv("DEVTOOLSAI_URL"), prodUrl));

        client = this.serverURL.equals(HttpUrl.parse("https://smartdriver.dev-tools.ai")) ? NetUtils.unsafeClient() : NetUtils.basicClient().build();
        BufferedImage im = ImageIO.read(driver.getScreenshotAs(OutputType.FILE));
        imSize = new Dimension(im.getWidth(), im.getHeight());

        if (isMobileWeb) {
            float windowHeight = getWebWindowHeight();
            float windowWidth = getWebWindowWidth();
            windowSize = new Dimension((int) windowWidth, (int) windowHeight);
            multiplier = 2.0;
        } else {
            windowSize = driver.manage().window().getSize();
            multiplier = 1.0 * imSize.width / windowSize.width;
        }

        log.debug("The screen multiplier is " + multiplier);
        try
        {
            JsonObject payload = CollectionUtils.keyValuesToJO("api_key", apiKey, "os",
                    String.format("%s-%s-%s", System.getProperty("os.name"),
                            System.getProperty("os.version"),
                            System.getProperty("os.arch")),
                    "sdk_version", SDK_VERSION,
                    "language", String.format("java-%s", System.getProperty("java.version")),
                    "test_case_name", this.testCaseName,
                    "automation_name", automationName);
            log.debug(MessageFormatter.format("Checking in with: {}", payload.toString()).toString());

            JsonObject r = JsonUtils.responseAsJson(NetUtils.basicPOST(client, this.serverURL, "ping", payload));
            if (!JsonUtils.booleanFromJson(r, "success"))
                log.debug(MessageFormatter.format("Error during checkin, server said: {}", r.toString()).getMessage());
        }
        catch (Throwable e)
        {
            log.debug(MessageFormatter.format("Checkin failed catastrophically: {}", e.getMessage()).getMessage());
        }
    }

    private float getWebWindowHeight() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object res = js.executeScript("return window.innerHeight;");
        if (res instanceof Number) {
            return ((Number) res).floatValue();
        } else {
            return 0;
        }
    }

    private float getWebWindowWidth() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Object res = js.executeScript("return window.innerWidth;");
        if (res instanceof Number) {
            return ((Number) res).floatValue();
        } else {
            return 0;
        }
    }

    /**
     * Constructor, creates a new SmartDriver with the default server url (<a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>), non-interactive mode, and with training enabled.
     *
     * @param driver The {@code RemoteWebDriver} to wrap
     * @param apiKey Your API key, acquired from <a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>.
     * @throws IOException If there was an initialization error.
     */
    public SmartDriver(AppiumDriver<T> driver, String apiKey) throws IOException
    {
        this(driver, apiKey, new HashMap<String, Object>());
    }

    /**
     * Convenience method, implicitly wait for the specified amount of time.
     *
     * @param waitTime The number of seconds to implicitly wait.
     * @return This {@code SmartDriver}, for chaining convenience.
     */
    public SmartDriver implicitlyWait(long waitTime)
    {
        driver.manage().timeouts().implicitlyWait(waitTime, TimeUnit.SECONDS);
        return this;
    }

    public org.openqa.selenium.remote.Response execute(String command)
    {
        return driver.execute(command);
    }

    public org.openqa.selenium.remote.Response execute(String command, Map<String, ?> parameters)
    {
        return driver.execute(command, parameters);
    }

    public String getContext()
    {
        return driver.getContext();
    }

    public Set<String> getContextHandles()
    {
        return driver.getContextHandles();
    }

    public ExecuteMethod getExecuteMethod()
    {
        return driver.getExecuteMethod();
    }

    public ScreenOrientation getOrientation()
    {
        return driver.getOrientation();
    }

    public URL getRemoteAddress()
    {
        return driver.getRemoteAddress();
    }

    public boolean isBrowser()
    {
        return driver.isBrowser();
    }

    public Location location()
    {
        return driver.location();
    }

    public void rotate(DeviceRotation rotation)
    {
        driver.rotate(rotation);
    }

    public void rotate(ScreenOrientation orientation)
    {
        driver.rotate(orientation);
    }

    public DeviceRotation rotation()
    {
        return driver.rotation();
    }

    public void setLocation(Location location)
    {
        driver.setLocation(location);
    }


    public Object executeAsyncScript(String script, Object... args)
    {
        return driver.executeAsyncScript(script, args);
    }

    public Object executeScript(String script, Object... args)
    {
        return driver.executeScript(script, args);
    }

    public Keyboard getKeyboard()
    {
        return driver.getKeyboard();
    }

    public Mouse getMouse()
    {
        return driver.getMouse();
    }

    public WebDriver.Options manage()
    {
        return driver.manage();
    }

    public WebDriver.Navigation navigate()
    {
        return driver.navigate();
    }

    public WebDriver.TargetLocator switchTo()
    {
        return driver.switchTo();
    }

    public void get(String url)
    {
        driver.get(url);
    }

    public T findElement(By locator, String elementName)
    {
        if (elementName == null) {
            elementName = String.format("element_name_by_locator_%s", locator.toString().replace('.', '_').replace(' ', '_'));
        }
        try
        {
            T driverElement = driver.findElement(locator);
            if (driverElement != null)
            {
                String key = uploadScreenshotIfNecessary(elementName, driverElement);
                if (key != null) {
                    updateElement(driverElement, key, elementName, true);
                }
            }
            return driverElement;
        }
        catch (Throwable x)
        {
            log.info(MessageFormatter.format("Element '{}' was not found by Selenium, trying with Smartdriver...", elementName).getMessage());

            ClassifyResult<T> result = classify(elementName);
            if (result.e != null) {
                return result.e.realElement;
            } else {
                log.error(result.msg);
            }

            log.error(MessageFormatter.format("Smartdriver was also unable to find the element with name '{}'", elementName).getMessage());

            throw x;
        }
    }
    
    public MobileElement findElement(By locator)
    {
        return findElement(locator, null);
    }
    
    public List<T> findElements(By locator)
    {
        return driver.findElements(locator);
    }
    
    public Capabilities getCapabilities()
    {
        return driver.getCapabilities();
    }
    
    public CommandExecutor getCommandExecutor()
    {
        return driver.getCommandExecutor();
    }
    
    public String getCurrentUrl()
    {
        return driver.getCurrentUrl();
    }
    
    public ErrorHandler getErrorHandler()
    {
        return driver.getErrorHandler();
    }
    
    public FileDetector getFileDetector()
    {
        return driver.getFileDetector();
    }
    
    public String getPageSource()
    {
        return driver.getPageSource();
    }
    
    public <X> X getScreenshotAs(OutputType<X> outputType)
    {
        return driver.getScreenshotAs(outputType);
    }
    
    public SessionId getSessionId()
    {
        return driver.getSessionId();
    }
    
    public String getTitle()
    {
        return driver.getTitle();
    }
    
    public String getWindowHandle()
    {
        return driver.getWindowHandle();
    }
    
    public Set<String> getWindowHandles()
    {
        return driver.getWindowHandles();
    }
    
    public void perform(Collection<Sequence> actions)
    {
        driver.perform(actions);
    }
    
    public void quit()
    {
        driver.quit();
    }
    
    public void resetInputState()
    {
        driver.resetInputState();
    }
    
    public void setErrorHandler(ErrorHandler handler)
    {
        driver.setErrorHandler(handler);
    }
    
    public void setFileDetector(FileDetector detector)
    {
        driver.setFileDetector(detector);
    }
    
    public void setLogLevel(Level level)
    {
        driver.setLogLevel(level);
    }
    
    public String toString()
    {
        return driver.toString();
    }

    /**
     * Attempts to find an element by class name.
     *
     * @param using The class name of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByClassName(String using, String elementName)
    {
        return this.findElement(By.className(using), elementName);
    }

    /**
     * Attempts to find an element by class name.
     *
     * @param using The class name of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByClassName(String using)
    {
        return findElementByClassName(using, null);
    }

    /**
     * Attempts to find all elements with the matching class name.
     *
     * @param using The class name of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByClassName(String using)
    {
        return driver.findElements(By.className(using));
    }

    /**
     * Attempts to find an element by css selector.
     *
     * @param using The css selector of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByCssSelector(String using, String elementName)
    {
        return this.findElement(By.cssSelector(using), elementName);
    }

    /**
     * Attempts to find an element by css selector.
     *
     * @param using The css selector of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByCssSelector(String using)
    {
        return findElementByCssSelector(using, null);
    }

    /**
     * Attempts to find all elements with the matching css selector.
     *
     * @param using The css selector of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByCssSelector(String using)
    {
        return driver.findElements(By.cssSelector(using));
    }

    /**
     * Attempts to find an element by id.
     *
     * @param using The id of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementById(String using, String elementName)
    {
        return findElement(By.id(using), elementName);
    }

    /**
     * Attempts to find an element by id.
     *
     * @param using The id of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementById(String using)
    {
        return findElementById(using, null);
    }

    /**
     * Attempts to find all elements with the matching id.
     *
     * @param using The id of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsById(String using)
    {
        return driver.findElements(By.id(using));
    }

    /**
     * Attempts to find an element by link text.
     *
     * @param using The link text of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByLinkText(String using, String elementName)
    {
        return findElement(By.linkText(using), elementName);
    }

    /**
     * Attempts to find an element by link text.
     *
     * @param using The link text of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByLinkText(String using)
    {
        return findElementByLinkText(using, null);
    }

    /**
     * Attempts to find all elements with the matching link text.
     *
     * @param using The link text of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByLinkText(String using)
    {
        return driver.findElements(By.linkText(using));
    }

    /**
     * Attempts to find an element by name.
     *
     * @param using The name of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByName(String using, String elementName)
    {
        return findElement(By.name(using), elementName);
    }

    /**
     * Attempts to find an element by name.
     *
     * @param using The name of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByName(String using)
    {
        return findElementByName(using, null);
    }

    /**
     * Attempts to find all elements with the matching name.
     *
     * @param using The name of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByName(String using)
    {
        return driver.findElements(By.name(using));
    }

    /**
     * Attempts to find an element by partial link text.
     *
     * @param using The partial link text of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByPartialLinkText(String using, String elementName)
    {
        return findElement(By.partialLinkText(using), elementName);
    }

    /**
     * Attempts to find an element by partial link text.
     *
     * @param using The partial link text of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByPartialLinkText(String using)
    {
        return findElementByPartialLinkText(using, null);
    }

    /**
     * Attempts to find all elements with the matching partial link text.
     *
     * @param using The partial link text of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByPartialLinkText(String using)
    {
        return driver.findElements(By.partialLinkText(using));
    }

    /**
     * Attempts to find an element by tag name.
     *
     * @param using The tag name of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByTagName(String using, String elementName)
    {
        return findElement(By.tagName(using), elementName);
    }

    /**
     * Attempts to find an element by tag name.
     *
     * @param using The tag name of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByTagName(String using)
    {
        return findElementByTagName(using, null);
    }

    /**
     * Attempts to find all elements with the matching tag name.
     *
     * @param using The tag name of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByTagName(String using)
    {
        return driver.findElements(By.tagName(using));
    }

    /**
     * Attempts to find an element by xpath.
     *
     * @param using The xpath of the element to find
     * @param elementName The label name of the element to be classified. Optional, set {@code null} to auto generate an element name.
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByXPath(String using, String elementName)
    {
        return findElement(By.xpath(using), elementName);
    }

    /**
     * Attempts to find an element by xpath.
     *
     * @param using The xpath of the element to find
     * @return The element that was found. Raises an exception otherwise.
     */
    public T findElementByXPath(String using)
    {
        return findElementByXPath(using, null);
    }

    /**
     * Attempts to find all elements with the matching xpath.
     *
     * @param using The xpath of the elements to find.
     * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
     */
    public List<T> findElementsByXPath(String using)
    {
        return driver.findElements(By.xpath(using));
    }

    /**
     * Finds an element by {@code elementName}. Please use {@link #findElementByElementName(String)} instead.
     *
     * @param elementName The label name of the element to be classified.
     * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
     */
    public T findByElementName(String elementName)
    {
        return findElementByElementName(elementName);
    }

    /**
     * Finds an element by {@code elementName}.
     *
     * @param elementName The label name of the element to be classified.
     * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
     */
    public T findElementByElementName(String elementName)
    {
        ClassifyResult<T> r = classify(elementName);
        if (r.e == null)
            throw new NoSuchElementException(r.msg);

        return r.e.realElement;
    }

    /**
     * Finds an elements by {@code elementName}. Uses visual AI to find the element.
     *
     * @param elementName The label name of the element to be classified.
     * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
     */
    public T findByAI(String elementName)
    {
        return findElementByElementName(elementName);
    }

    private String getScreenshotHash(String screenshotBase64) {
        try {


            /* the following code does not work because ImageIO.write does not create the same output as cv2 for the same b64 image, so the hashes are not good
            // Convert b64 image to buffered image, crop top 85 pixels, convert back to b64
            BufferedImage image = null;
            image = ImageIO.read(new ByteArrayInputStream(
                    Base64.getDecoder().decode(screenshotBase64.replaceAll("\\r\\n|\\r|\\n", ""))));
            image = image.getSubimage(0, 85, image.getWidth(), image.getHeight() - 85);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            screenshotBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            */
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] md5sum = md.digest(screenshotBase64.getBytes(StandardCharsets.UTF_8));
            String output = String.format("%032X", new BigInteger(1, md5sum));
            return output.toLowerCase();
        } catch (Throwable e) {
            return "";
        }
    }

    private JsonObject checkScreenshotExists(String screenshotUUID, String elementName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);
        payload.addProperty("screenshot_uuid", screenshotUUID);
        payload.add("stack_trace", Utils.collectStackTrace());
        try {
            JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "exists_screenshot", payload));
            return res;
        } catch (Throwable e) {
            log.debug("Error checking if screenshot exists");
            e.printStackTrace();
            return null;
        }
    }

    private JsonObject uploadScreenshot(String screenshotBase64,String elementName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);
        payload.addProperty("screenshot", screenshotBase64);
        payload.addProperty("test_case_name", testCaseName);
        payload.addProperty("is_appium", true);
        try {
            JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "upload_screenshot", payload));
            return res;
        } catch (Throwable e) {
            log.debug("Error uploading screenshot");
            e.printStackTrace();
            return null;
        }
    }

    private JsonObject uploadTCScreenshot(String screenshotBase64, String elementName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);
        payload.addProperty("screenshot", screenshotBase64);
        payload.addProperty("test_case_name", testCaseName);
        payload.addProperty("is_interactive", true);
        payload.addProperty("is_appium", true);

        try {
            JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "upload_screenshot", payload));
            return res;
        } catch (Throwable e) {
            log.debug("Error uploading test case screenshot");
            e.printStackTrace();
            return null;
        }
    }

    public void scrollToElement(T element, Boolean scrollUp) {
        /*
        if(scrollUp) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(false);", element);
        } else {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        }*/
    }

    public void scrollPage(int amount) {
        /*
        ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, " + amount + ")");
         */
    }

    private String uploadScreenshotIfNecessary(String elementName, T element)	{
        Boolean isElementFrozen = checkIfFrozen(elementName);
        if (isElementFrozen) {
            return null;
        } else {
            String screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
            String screenshotUUID = getScreenshotHash(screenshotBase64);
            refScreenshotUUID = null;
            pageOffset = 0f;
            if (element != null) {
                pageOffset = getPageOffset();
                Boolean needsToScroll = (element.getRect().getY() > (windowSize.getHeight() + pageOffset)) || (element.getRect().getY() < pageOffset);
                if(needsToScroll) {
                    previousPageOffset = pageOffset;
                    refScreenshotUUID = screenshotUUID;

                    scrollToElement(element, element.getRect().getY() < pageOffset);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                    screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
                    screenshotUUID = getScreenshotHash(screenshotBase64);
                    pageOffset = getPageOffset();
                    scrollPage((int) (previousPageOffset - pageOffset));
                }
            }


            JsonObject screenshotExistsResponse = checkScreenshotExists(screenshotUUID, elementName);
            if (screenshotExistsResponse != null && screenshotExistsResponse.get("exists_screenshot").getAsBoolean()) {
                return screenshotUUID;
            } else {
                JsonObject uploadScreenshotResponse = uploadScreenshot(screenshotBase64, elementName);
                if (uploadScreenshotResponse != null) {
                    if (uploadScreenshotResponse.get("success").getAsBoolean()) {
                        return uploadScreenshotResponse.get("screenshot_uuid").getAsString();
                    } else {
                        log.info("Error uploading screenshot");
                        return screenshotUUID;
                    }
                } else {
                    log.info("Error uploading screenshot");
                    return screenshotUUID;
                }
            }
        }
    }

    private Boolean checkIfFrozen(String elementName) {
        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);

        try {
            JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "check_frozen", payload));
            return res.get("is_frozen").getAsBoolean();
        } catch (Throwable e) {
            log.debug("Error checking if element is frozen");
            e.printStackTrace();
            return true;
        }
    }
    /**
     * Shared {@code findElementBy} functionality. This serves as the base logic for most find by methods exposed to the end user.
     *
     * @param using The search term to use when looking for an element.
     * @param elementName The label name of the element to be classified. This is what the element will be stored under in the dev-tools.ai db.
     * @param shortcode The short identifier for the type of lookup being performed. This will be used to aut-generate an {@code elementName} if the user did not specify one.
     * @param fn The selenium function to call with {@code using}, which will be used to fetch what selenium thinks is the target element.
     * @return The SmartDriverElement
     */
    private T findElementByGeneric(String using, String elementName, String shortcode, Function<String, T> fn)
    {
        if (elementName == null) {
            elementName = String.format("element_name_by_%s_%s", shortcode, using.replace('.', '_'));
        }
        try
        {
            T driverElement = fn.apply(using);
            if (driverElement != null)
            {
                String key = uploadScreenshotIfNecessary(elementName, driverElement);
                if (key != null) {
                    updateElement(driverElement, key, elementName, true);
                }
            }
            return driverElement;
        }
        catch (Throwable x)
        {
            log.info(MessageFormatter.format("Element '{}' was not found by Selenium, trying with Smartdriver...", elementName).getMessage());

            ClassifyResult<T> result = classify(elementName);
            if (result.e != null) {
                return result.e.realElement;
            } else {
                log.error(result.msg);
            }

            log.error(MessageFormatter.format("Smartdriver was also unable to find the element with name '{}'", elementName).getMessage());

            throw x;
        }
    }

    /**
     * Updates the entry for an element as it is known to the dev-tools.ai servers.
     *
     * @param elem The element to update
     * @param screenshotUUID The key associated with this element
     * @param elementName The name associated with this element
     * @param trainIfNecessary Set {@code true} if the model on the server should also be trained with this element.
     */
    protected void updateElement(T elem, String screenshotUUID, String elementName, boolean trainIfNecessary)
    {
        Rectangle rect = elem.getRect();

        JsonObject payload = new JsonObject();
        payload.addProperty("screenshot_uuid", screenshotUUID);
        payload.addProperty("retrain", trainIfNecessary);
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);
        payload.addProperty("x", rect.x * multiplier);
        payload.addProperty("y", rect.y * multiplier);
        payload.addProperty("width", rect.width * multiplier);
        payload.addProperty("height", rect.height * multiplier);
        payload.addProperty("multiplier", multiplier);
        payload.addProperty("test_case_name", testCaseName);
        payload.addProperty("page_offset", this.pageOffset * this.multiplier);
        payload.addProperty("ref_screenshot_uuid", this.refScreenshotUUID);

        try {
            JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "add_action_info", payload));
        } catch (Throwable e) {
            log.debug("Error updating element");
            e.printStackTrace();
        }
    }

    private CollectionUtils.Tuple<JsonObject, Boolean> getTCBox(String elementName, String eventUUID) {
        JsonObject payload = new JsonObject();
        payload.addProperty("api_key", apiKey);
        payload.addProperty("label", elementName);
        payload.addProperty("screenshot_uuid", lastTestCaseScreenshotUUID);
        payload.addProperty("run_classifier", useClassifierDuringCreation);
        payload.addProperty("event_id", eventUUID);
        payload.add("stack_trace", Utils.collectStackTrace());
        Boolean needsReload = false;

        try {
            JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "testcase/get_action_info", payload));
            needsReload = res.get("needs_reload").getAsBoolean();
            return new CollectionUtils.Tuple<>(res, needsReload);
        } catch (Throwable e) {
            log.debug("Error getting TC box");
            e.printStackTrace();
            return new CollectionUtils.Tuple<>(null, needsReload);
        }
    }

    private void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else if (os.contains("windows")) {
                Runtime.getRuntime().exec("run " + url);
            } else {
                log.info(MessageFormatter.format("Please open the following URL in your browser: {}", url).getMessage());
            }
        } catch (Throwable e) {
            log.info(MessageFormatter.format("Please open the following URL in your browser: {}", url).getMessage());
        }
    }

    /**
     * Perform additional classification on an element by querying the dev-tools.ai server.
     *
     * @param elementName The name of the element to run classification on.
     * @return The result of the classification.
     */
    protected ClassifyResult<T> classify(String elementName)
    {
        if(testCaseCreationMode) {
            String screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
            JsonObject res = uploadTCScreenshot(screenshotBase64, elementName);

            if (res.get("success").getAsBoolean()) {
                lastTestCaseScreenshotUUID = res.get("screenshot_uuid").getAsString();
                CollectionUtils.Tuple<JsonObject, Boolean> boxResponseTp = getTCBox(elementName, null);
                JsonObject boxResponse = boxResponseTp.k;
                Boolean needsReload = boxResponseTp.v;

                if (boxResponse != null && boxResponse.get("success").getAsBoolean() && boxResponse.get("predicted_element") != JsonNull.INSTANCE) {
                    return new ClassifyResult(new SmartDriverElement(boxResponse.get("predicted_element").getAsJsonObject(), this, getPageOffset()), lastTestCaseScreenshotUUID);
                } else {
                    // label_url = self.url + '/testcase/label?test_case_name=' + urllib.parse.quote(self.test_case_uuid)

                    // generate a uuid
                    String eventUUID = UUID.randomUUID().toString();
                    String labelUrl = serverURL + "/testcase/label?test_case_name=" + URLEncoder.encode(testCaseName) + "&event_id=" + eventUUID + "&api_key=" + apiKey;
                    openBrowser(labelUrl);
                    while (true) {
                        boxResponseTp = getTCBox(elementName, eventUUID);
                        boxResponse = boxResponseTp.k;
                        needsReload = boxResponseTp.v;
                        if (boxResponse != null && boxResponse.get("success").getAsBoolean() && boxResponse.get("predicted_element") != JsonNull.INSTANCE) {
                            return new ClassifyResult(new SmartDriverElement(boxResponse.get("predicted_element").getAsJsonObject(), this, getPageOffset()), lastTestCaseScreenshotUUID);
                        }
                        if (needsReload) {
                            screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
                            uploadTCScreenshot(screenshotBase64, elementName);
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                log.info("Failed to upload test case screenshot");
                log.info(res.get("message").getAsString());
                return new ClassifyResult(null, lastTestCaseScreenshotUUID, res.get("message").getAsString());
            }
        } else {
            String pageSource = "", msg = "Smartdriver driver exception", key = null;
            try {
                String screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
                String screenshotUUID = getScreenshotHash(screenshotBase64);
                JsonObject screenshotExistsResponse = checkScreenshotExists(screenshotUUID, elementName);

                if (screenshotExistsResponse != null && screenshotExistsResponse.get("success").getAsBoolean() && screenshotExistsResponse.get("predicted_element") != JsonNull.INSTANCE) {
                    msg = screenshotExistsResponse.get("message").getAsString();
                    log.info(msg);
                    float currentOffset = getPageOffset();
                    float bottomOffset = currentOffset + windowSize.height;
                    float realOffset = (float) (screenshotExistsResponse.get("page_offset").getAsFloat() / multiplier);
                    if (realOffset > bottomOffset || realOffset < currentOffset) {
                        int scrollOffset = (int) (realOffset - currentOffset);
                        // Scroll
                        scrollPage((int) scrollOffset);
                        Thread.sleep(1000);
                        screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
                        screenshotUUID = getScreenshotHash(screenshotBase64);
                        screenshotExistsResponse = checkScreenshotExists(screenshotUUID, elementName);
                    }
                    if (screenshotExistsResponse != null && screenshotExistsResponse.get("success").getAsBoolean() && screenshotExistsResponse.get("predicted_element") != JsonNull.INSTANCE) {
                        return new ClassifyResult(new SmartDriverElement(screenshotExistsResponse.get("predicted_element").getAsJsonObject(), this, getPageOffset()), null);
                    }
                }
                JsonObject payload = new JsonObject();
                payload.addProperty("api_key", apiKey);
                payload.addProperty("label", elementName);
                payload.addProperty("screenshot", screenshotBase64);
                payload.addProperty("test_case_name", testCaseName);
                payload.add("stack_trace", Utils.collectStackTrace());

                JsonObject classifyResponse = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "detect", payload));

                if (!classifyResponse.get("success").getAsBoolean()) {
                    classifyResponse = classifyFullScreen(elementName, screenshotBase64);
                    if (!classifyResponse.get("success").getAsBoolean()) {
                        log.info(classifyResponse.get("message").getAsString());
                        return new ClassifyResult(null, null, classifyResponse.get("message").getAsString());
                    }
                }
                msg = classifyResponse.get("message").getAsString().replace(prodUrl, serverURL.toString());
                log.info(msg);
                try {
                    return new ClassifyResult(new SmartDriverElement(classifyResponse.get("predicted_element").getAsJsonObject(), this, getPageOffset()),
                            classifyResponse.get("screenshot_uuid").getAsString());
                } catch (Throwable e) {
                    log.error("Error creating SmartDriverElement from response");
                    e.printStackTrace();
                    return new ClassifyResult(null, key, msg);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            }

            log.warn(msg);
            return new ClassifyResult(null, key, msg);
        }
    }

    private float getPageOffset(){
        /*
        Object res = driver.executeScript("return window.pageYOffset;");
        if (res instanceof Number) {
            return ((Number) res).floatValue();
        } else {
            return 0;
        }
        */
        return 0;
    }

    JsonObject classifyFullScreen(String elementName, String screenshotBase64) {
        int lastOffset = -1;
        int offset = 1;
        int windowHeight = windowSize.height;
        scrollPage(-100000);
        JsonObject r = new JsonObject();
        r.addProperty("success", false);

        while(offset > lastOffset) {
            lastOffset = offset;
            screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
            JsonObject payload = new JsonObject();
            payload.addProperty("api_key", apiKey);
            payload.addProperty("label", elementName);
            payload.addProperty("screenshot", screenshotBase64);
            payload.addProperty("test_case_name", testCaseName);
            payload.add("stack_trace", Utils.collectStackTrace());

            try {
                r = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "detect", payload));
                if (r.get("success").getAsBoolean() || !isMobileWeb) {
                    return r;
                }
            } catch (Throwable e) {
                log.error("Error creating SmartDriverElement from response");
                e.printStackTrace();
                return r;
            }
            scrollPage((int) windowHeight);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            offset = (int) getPageOffset();
        }
        return r;
    }

    /**
     * Simple container for encapsulating results of calls to {@code classify()}.
     *
     */
    private static class ClassifyResult<T extends MobileElement>
    {
        /**
         * The SmartDriverElement created by the call to classify
         */
        public SmartDriverElement<T> e;

        /**
         * The key returned by the call to classify
         */
        public String key;

        /**
         * The message associated with this result
         */
        public String msg;

        /**
         * Constructor, creates a new ClassifyResult.
         *
         * @param e The SmartDriverElement to to use
         * @param key The key to use
         * @param msg The message to associate with this result
         */
        ClassifyResult(SmartDriverElement<T> e, String key, String msg)
        {
            this.e = e;
            this.key = key;
            this.msg = msg;
        }

        /**
         * Constructor, creates a new ClassifyResult, where the {@code msg} is set to the empty String by default.
         *
         * @param e
         * @param key
         */
        ClassifyResult(SmartDriverElement<T> e, String key)
        {
            this(e, key, "");
        }
    }

    public void close() {
        driver.close();
    }
}
