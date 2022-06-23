package ai.devtools.sdk;

import com.google.gson.JsonObject;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.RemoteWebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An enhanced RemoteWebElement which uses the results of the dev-tools.ai classifier for improved accuracy.
 *
 */
public class SmartDriverElement extends RemoteWebElement
{
	/**
	 * The logger for this class
	 */
	private static Logger log = LoggerFactory.getLogger(SmartDriverElement.class);

	/**
	 * The webdriver the user is using. We wrap this for when the user calls methods that interact with selenium.
	 */
	private RemoteWebDriver driver;

	/**
	 * The underlying {@code WebElement} used for performing actions in the browser.
	 */
	private WebElement realElement;

	/**
	 * The text in this element, as determined by dev-tools.ai's classifier
	 */
	private String text;

	/**
	 * The size of this element, in pixels
	 */
	private Dimension size;

	/**
	 * The location of this element, in pixels (offset from the upper left corner of the screen)
	 */
	private Point location;

	/**
	 * The rectangle that can be drawn around this element. Basically combines size and location.
	 */
	private Rectangle rectangle;

	/**
	 * The tag name of this element, as determined by dev-tools.ai's classifier
	 */
	private String tagName;

	/**
	 * Constructor, creates a new SmartDriverElement
	 * 
	 * @param elem The element data returned by the FD API, as JSON
	 * @param driver The {@code SmartDriver} to associate with this {@code SmartDriverElement}.
	 */
	SmartDriverElement(JsonObject elem, SmartDriver driver)
	{
		log.debug("Creating new SmartDriverElement w/ {}", elem);

		this.driver = driver.driver;
		this.realElement = MatchUtils.matchBoundingBoxToSeleniumElement(elem, driver);

		text = JsonUtils.stringFromJson(elem, "text");
		size = new Dimension(JsonUtils.intFromJson(elem, "width") / (int) driver.multiplier, JsonUtils.intFromJson(elem, "height") / (int) driver.multiplier);

		location = new Point(JsonUtils.intFromJson(elem, "x") / (int) driver.multiplier, JsonUtils.intFromJson(elem, "y") / (int) driver.multiplier);

		// this.property = property //TODO: not referenced/implemented on python side??
		rectangle = new Rectangle(location, size);
		tagName = JsonUtils.stringFromJson(elem, "class");

	}

	public String toString()
	{
		return "SmartDriverElement: " + text;
	}

	@Override
	public String getText()
	{
		return text;
	}

	@Override
	public Dimension getSize()
	{
		return size;
	}

	@Override
	public Point getLocation()
	{
		return location;
	}

	@Override
	public Rectangle getRect()
	{
		return rectangle;
	}

	@Override
	public String getTagName()
	{
		return tagName;
	}

	@Override
	public void clear()
	{
		realElement.clear();
	}

	@Override
	public WebElement findElement(By by)
	{
		return driver.findElement(by);
	}

	@Override
	public List<WebElement> findElements(By by)
	{
		return driver.findElements(by);
	}

	@Override
	public String getAttribute(String name)
	{
		return realElement.getAttribute(name);
	}

	@Override
	public String getCssValue(String propertyName)
	{
		return realElement.getCssValue(propertyName);
	}

	@Override
	public boolean isDisplayed()
	{
		return realElement.isDisplayed();
	}

	@Override
	public boolean isEnabled()
	{
		return realElement.isEnabled();
	}

	@Override
	public boolean isSelected()
	{
		return realElement.isSelected();
	}

	@Override
	public void click()
	{
		realElement.click();
	}

	@Override
	public void sendKeys(CharSequence... keysToSend)
	{
		realElement.sendKeys(keysToSend);
	}

	@Override
	public void submit()
	{
		realElement.submit();
	}
}
