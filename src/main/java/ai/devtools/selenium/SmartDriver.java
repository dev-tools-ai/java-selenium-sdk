package ai.devtools.selenium;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.google.gson.JsonNull;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;


import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.slf4j.helpers.MessageFormatter;

import com.google.gson.JsonObject;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * The {@code SmartDriver} class is a wrapper around a {@code RemoteWebDriver} that uses the results of the dev-tools.ai classifier for improved robustness, finding elements visually and avoiding broken selectors.
 */
@SuppressWarnings("deprecation")
public class SmartDriver extends RemoteWebDriver {
	/**
	 * The current version of the SDK
	 */
	private static String SDK_VERSION = "0.1.9";

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
	RemoteWebDriver driver;

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
	double multiplier;
	private Boolean useClassifierDuringCreation;
	private Boolean testCaseCreationMode;

	/**
	 * Constructor, creates a new SmartDriver.
	 *
	 * @param driver The {@code RemoteWebDriver} to wrap
	 * @param apiKey Your API key, acquired from <a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>.
	 * @param initializationDict The configuration options for the driver.
	 * @throws IOException If there was an initialization error.
	 */
	public SmartDriver(RemoteWebDriver driver, String apiKey, Map<String, Object> initializationDict) throws IOException
	{
		this.driver = driver;
		this.apiKey = apiKey;
		log.setLevel(org.apache.log4j.Level.INFO);
		BasicConfigurator.configure();

		this.testCaseName = (String) initializationDict.get("testCaseName");
		this.useClassifierDuringCreation = (Boolean) initializationDict.get("useClassifierDuringCreation");
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
		multiplier = 1.0 * ImageIO.read(driver.getScreenshotAs(OutputType.FILE)).getWidth() / driver.manage().window().getSize().width;

		log.debug("The screen multiplier is " + multiplier);

		try
		{
			JsonObject payload = CollectionUtils.keyValuesToJO("api_key", apiKey, "os",
					String.format("%s-%s-%s", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch")), "sdk_version", SDK_VERSION, "language",
					String.format("java-%s", System.getProperty("java.version")), "test_case_name", this.testCaseName);
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

	/**
	 * Constructor, creates a new SmartDriver with the default server url (<a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>), non-interactive mode, and with training enabled.
	 *
	 * @param driver The {@code RemoteWebDriver} to wrap
	 * @param apiKey Your API key, acquired from <a href="https://smartdriver.dev-tools.ai">smartdriver.dev-tools.ai</a>.
	 * @throws IOException If there was an initialization error.
	 */
	public SmartDriver(RemoteWebDriver driver, String apiKey) throws IOException
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

	@Override
	public Object executeAsyncScript(String script, Object... args)
	{
		return driver.executeAsyncScript(script, args);
	}

	@Override
	public Object executeScript(String script, Object... args)
	{
		return driver.executeScript(script, args);
	}

	/**
	 * Opens a web browser and directs it to {@code url}.
	 *
	 * @param url The URL to launch the browser to.
	 */
	@Override
	public void get(String url)
	{
		driver.get(url);
	}

	public WebElement findElement(By locator, String elementName)
	{
		if (elementName == null) {
			elementName = String.format("element_name_by_locator_%s", locator.toString().replace('.', '_').replace(' ', '_'));
		}
		try
		{
			WebElement driverElement = driver.findElement(locator);
			if (driverElement != null)
			{
				String key = uploadScreenshotIfNecessary(elementName);
				if (key != null) {
					updateElement(driverElement, key, elementName, true);
				}
			}
			return driverElement;
		}
		catch (Throwable x)
		{
			log.info(MessageFormatter.format("Element '{}' was not found by Selenium, trying with Smartdriver...", elementName).getMessage());

			ClassifyResult result = classify(elementName);
			if (result.e != null) {
				return result.e;
			} else {
				log.error(result.msg);
			}

			log.error(MessageFormatter.format("Smartdriver was also unable to find the element with name '{}'", elementName).getMessage());

			throw x;
		}
	}

	@Override
	public WebElement findElement(By locator)
	{
		return findElement(locator, null);
	}

	@Override
	public List<WebElement> findElements(By locator)
	{
		return driver.findElements(locator);
	}

	@Override
	public Capabilities getCapabilities()
	{
		return driver.getCapabilities();
	}

	@Override
	public CommandExecutor getCommandExecutor()
	{
		return driver.getCommandExecutor();
	}

	@Override
	public String getCurrentUrl()
	{
		return driver.getCurrentUrl();
	}

	@Override
	public ErrorHandler getErrorHandler()
	{
		return driver.getErrorHandler();
	}

	@Override
	public FileDetector getFileDetector()
	{
		return driver.getFileDetector();
	}

	@Override
	public String getPageSource()
	{
		return driver.getPageSource();
	}

	@Override
	public <X> X getScreenshotAs(OutputType<X> outputType)
	{
		return driver.getScreenshotAs(outputType);
	}

	@Override
	public SessionId getSessionId()
	{
		return driver.getSessionId();
	}

	@Override
	public String getTitle()
	{
		return driver.getTitle();
	}

	@Override
	public String getWindowHandle()
	{
		return driver.getWindowHandle();
	}

	@Override
	public Set<String> getWindowHandles()
	{
		return driver.getWindowHandles();
	}

	@Override
	public Options manage()
	{
		return driver.manage();
	}

	@Override
	public Navigation navigate()
	{
		return driver.navigate();
	}

	@Override
	public void perform(Collection<Sequence> actions)
	{
		driver.perform(actions);
	}

	@Override
	public void quit()
	{
		driver.quit();
	}

	@Override
	public void resetInputState()
	{
		driver.resetInputState();
	}

	@Override
	public void setErrorHandler(ErrorHandler handler)
	{
		driver.setErrorHandler(handler);
	}

	@Override
	public void setFileDetector(FileDetector detector)
	{
		driver.setFileDetector(detector);
	}

	@Override
	public void setLogLevel(Level level)
	{
		driver.setLogLevel(level);
	}

	@Override
	public TargetLocator switchTo()
	{
		return driver.switchTo();
	}

	@Override
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
	public WebElement findElementByClassName(String using, String elementName)
	{
		return this.findElement(By.className(using), elementName);
	}

	/**
	 * Attempts to find an element by class name.
	 *
	 * @param using The class name of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByClassName(String using)
	{
		return findElementByClassName(using, null);
	}

	/**
	 * Attempts to find all elements with the matching class name.
	 *
	 * @param using The class name of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByClassName(String using)
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
	public WebElement findElementByCssSelector(String using, String elementName)
	{
		return this.findElement(By.cssSelector(using), elementName);
	}

	/**
	 * Attempts to find an element by css selector.
	 *
	 * @param using The css selector of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByCssSelector(String using)
	{
		return findElementByCssSelector(using, null);
	}

	/**
	 * Attempts to find all elements with the matching css selector.
	 *
	 * @param using The css selector of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByCssSelector(String using)
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
	public WebElement findElementById(String using, String elementName)
	{
		return findElement(By.id(using), elementName);
	}

	/**
	 * Attempts to find an element by id.
	 *
	 * @param using The id of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementById(String using)
	{
		return findElementById(using, null);
	}

	/**
	 * Attempts to find all elements with the matching id.
	 *
	 * @param using The id of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsById(String using)
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
	public WebElement findElementByLinkText(String using, String elementName)
	{
		return findElement(By.linkText(using), elementName);
	}

	/**
	 * Attempts to find an element by link text.
	 *
	 * @param using The link text of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByLinkText(String using)
	{
		return findElementByLinkText(using, null);
	}

	/**
	 * Attempts to find all elements with the matching link text.
	 *
	 * @param using The link text of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByLinkText(String using)
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
	public WebElement findElementByName(String using, String elementName)
	{
		return findElement(By.name(using), elementName);
	}

	/**
	 * Attempts to find an element by name.
	 *
	 * @param using The name of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByName(String using)
	{
		return findElementByName(using, null);
	}

	/**
	 * Attempts to find all elements with the matching name.
	 *
	 * @param using The name of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByName(String using)
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
	public WebElement findElementByPartialLinkText(String using, String elementName)
	{
		return findElement(By.partialLinkText(using), elementName);
	}

	/**
	 * Attempts to find an element by partial link text.
	 *
	 * @param using The partial link text of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByPartialLinkText(String using)
	{
		return findElementByPartialLinkText(using, null);
	}

	/**
	 * Attempts to find all elements with the matching partial link text.
	 *
	 * @param using The partial link text of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByPartialLinkText(String using)
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
	public WebElement findElementByTagName(String using, String elementName)
	{
		return findElement(By.tagName(using), elementName);
	}

	/**
	 * Attempts to find an element by tag name.
	 *
	 * @param using The tag name of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByTagName(String using)
	{
		return findElementByTagName(using, null);
	}

	/**
	 * Attempts to find all elements with the matching tag name.
	 *
	 * @param using The tag name of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByTagName(String using)
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
	public WebElement findElementByXPath(String using, String elementName)
	{
		return findElement(By.xpath(using), elementName);
	}

	/**
	 * Attempts to find an element by xpath.
	 *
	 * @param using The xpath of the element to find
	 * @return The element that was found. Raises an exception otherwise.
	 */
	public WebElement findElementByXPath(String using)
	{
		return findElementByXPath(using, null);
	}

	/**
	 * Attempts to find all elements with the matching xpath.
	 *
	 * @param using The xpath of the elements to find.
	 * @return A {@code List} with any elements that were found, or an empty {@code List} if no matches were found.
	 */
	public List<WebElement> findElementsByXPath(String using)
	{
		return driver.findElements(By.xpath(using));
	}

	/**
	 * Finds an element by {@code elementName}. Please use {@link #findElementByElementName(String)} instead.
	 *
	 * @param elementName The label name of the element to be classified.
	 * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
	 */
	public WebElement findByElementName(String elementName)
	{
		return findElementByElementName(elementName);
	}

	/**
	 * Finds an element by {@code elementName}.
	 *
	 * @param elementName The label name of the element to be classified.
	 * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
	 */
	public WebElement findElementByElementName(String elementName)
	{
		ClassifyResult r = classify(elementName);
		if (r.e == null)
			throw new NoSuchElementException(r.msg);

		return r.e;
	}

	/**
	 * Finds an elements by {@code elementName}. Uses visual AI to find the element.
	 *
	 * @param elementName The label name of the element to be classified.
	 * @return An element associated with {@code elementName}. Throws NoSuchElementException otherwise.
	 */
	public WebElement findByAI(String elementName)
	{
		return findElementByElementName(elementName);
	}

	private String getScreenshotHash(String screenshotBase64) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] md5sum = md.digest(screenshotBase64.getBytes());
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

		try {
			JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "upload_screenshot", payload));
			return res;
		} catch (Throwable e) {
			log.debug("Error uploading test case screenshot");
			e.printStackTrace();
			return null;
		}
	}

	private String uploadScreenshotIfNecessary(String elementName)	{
		Boolean isElementFrozen = checkIfFrozen(elementName);
		if (isElementFrozen) {
			return null;
		} else {
			String screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
			String screenshotUUID = getScreenshotHash(screenshotBase64);
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
	private WebElement findElementByGeneric(String using, String elementName, String shortcode, Function<String, WebElement> fn)
	{
		if (elementName == null) {
			elementName = String.format("element_name_by_%s_%s", shortcode, using.replace('.', '_'));
		}
		try
		{
			WebElement driverElement = fn.apply(using);
			if (driverElement != null)
			{
				String key = uploadScreenshotIfNecessary(elementName);
				if (key != null) {
					updateElement(driverElement, key, elementName, true);
				}
			}
			return driverElement;
		}
		catch (Throwable x)
		{
			log.info(MessageFormatter.format("Element '{}' was not found by Selenium, trying with Smartdriver...", elementName).getMessage());

			ClassifyResult result = classify(elementName);
			if (result.e != null) {
				return result.e;
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
	protected void updateElement(WebElement elem, String screenshotUUID, String elementName, boolean trainIfNecessary)
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

		try {
			JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "add_action_info", payload));
		} catch (Throwable e) {
			log.debug("Error updating element");
			e.printStackTrace();
		}
	}

	private JsonObject getTCBox(String elementName) {
		JsonObject payload = new JsonObject();
		payload.addProperty("api_key", apiKey);
		payload.addProperty("label", elementName);
		payload.addProperty("screenshot_uuid", lastTestCaseScreenshotUUID);
		payload.addProperty("run_classifier", useClassifierDuringCreation);

		try {
			JsonObject res = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "testcase/get_action_info", payload));
			return res;
		} catch (Throwable e) {
			log.debug("Error getting TC box");
			e.printStackTrace();
			return null;
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
	protected ClassifyResult classify(String elementName)
	{
		if(testCaseCreationMode) {
			String screenshotBase64 = driver.getScreenshotAs(OutputType.BASE64);
			JsonObject res = uploadTCScreenshot(screenshotBase64, elementName);
			if (res.get("success").getAsBoolean()) {
				lastTestCaseScreenshotUUID = res.get("screenshot_uuid").getAsString();
				JsonObject boxResponse = getTCBox(elementName);
				if (boxResponse != null && boxResponse.get("success").getAsBoolean() && boxResponse.get("predicted_element") != JsonNull.INSTANCE) {
					return new ClassifyResult(new SmartDriverElement(boxResponse.get("predicted_element").getAsJsonObject(), this), lastTestCaseScreenshotUUID);
				} else {
					// label_url = self.url + '/testcase/label?test_case_name=' + urllib.parse.quote(self.test_case_uuid)
					String labelUrl = serverURL + "/testcase/label?test_case_name=" + URLEncoder.encode(testCaseName);
					openBrowser(labelUrl);
					while (true) {
						boxResponse = getTCBox(elementName);
						if (boxResponse != null && boxResponse.get("success").getAsBoolean() && boxResponse.get("predicted_element") != JsonNull.INSTANCE) {
							return new ClassifyResult(new SmartDriverElement(boxResponse.get("predicted_element").getAsJsonObject(), this), lastTestCaseScreenshotUUID);
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
					return new ClassifyResult(new SmartDriverElement(screenshotExistsResponse.get("predicted_element").getAsJsonObject(), this),
								null);
				} else {
					JsonObject payload = new JsonObject();
					payload.addProperty("api_key", apiKey);
					payload.addProperty("label", elementName);
					payload.addProperty("screenshot", screenshotBase64);
					payload.addProperty("test_case_name", testCaseName);

					JsonObject classifyResponse = JsonUtils.responseAsJson(NetUtils.basicPOST(client, serverURL, "detect", payload));

					if (!classifyResponse.get("success").getAsBoolean()) {
						msg = classifyResponse.get("message").getAsString().replace(prodUrl, serverURL.toString());
						return new ClassifyResult(null, key, msg);
					} else {
						msg = classifyResponse.get("message").getAsString().replace(prodUrl, serverURL.toString());
						log.info(msg);
						try {
							return new ClassifyResult(new SmartDriverElement(classifyResponse.get("predicted_element").getAsJsonObject(), this),
									classifyResponse.get("screenshot_uuid").getAsString());
						} catch (Throwable e) {
							log.error("Error creating SmartDriverElement from response");
							e.printStackTrace();
							return new ClassifyResult(null, key, msg);
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}

			log.warn(msg);
			return new ClassifyResult(null, key, msg);
		}
	}

	/**
	 * Simple container for encapsulating results of calls to {@code classify()}.
	 *
	 */
	private static class ClassifyResult
	{
		/**
		 * The SmartDriverElement created by the call to classify
		 */
		public SmartDriverElement e;

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
		ClassifyResult(SmartDriverElement e, String key, String msg)
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
		ClassifyResult(SmartDriverElement e, String key)
		{
			this(e, key, "");
		}
	}

	@Override
	public void close() {
		driver.close();
	}
}
