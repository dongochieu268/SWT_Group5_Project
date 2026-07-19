package fu.swt301.sms.system;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StaffManagementSystemSeleniumIT {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/StaffManagement/";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String STAFF_NAME = "Nguyen Van Binh";
    private static final String STAFF_EMAIL = "binhnv@company.com";
    private static final String STAFF_PASSWORD = "binh1234";
    private static final int WAIT_SECONDS = 10;

    private static String baseUrl;
    private static Path evidenceDirectory;

    private WebDriver driver;
    private WebDriverWait wait;

    @BeforeClass
    public static void prepareSystemTestData() throws Exception {
        Assume.assumeTrue("Set -Dselenium.system.test=true to run Selenium system tests.",
                Boolean.parseBoolean(System.getProperty("selenium.system.test", "false")));
        baseUrl = normalizeBaseUrl(configValue("selenium.baseUrl",
                "SMS_SYSTEM_TEST_BASE_URL", DEFAULT_BASE_URL));
        evidenceDirectory = Paths.get("target", "selenium-system-test-evidence");
        Files.createDirectories(evidenceDirectory);

        verifyApplicationResponds();

        WebDriver setupDriver = createWebDriver();
        WebDriverWait setupWait = new WebDriverWait(setupDriver, Duration.ofSeconds(WAIT_SECONDS));
        try {
            login(setupDriver, setupWait, ADMIN_EMAIL, ADMIN_PASSWORD);
            ensureStaffAccountExists(setupDriver, setupWait);
        } finally {
            setupDriver.quit();
        }
    }

    @Before
    public void setUp() {
        driver = createWebDriver();
        wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SECONDS));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void case01_anonymousUserRedirectsToLoginFromStaffList() throws Exception {
        open("staff-list");

        waitForLoginPage();

        assertTrue(driver.getCurrentUrl().contains("/login"));
        assertBodyContains("Login");
        capture("B1_redirect_login_2_sau.png");
    }

    @Test
    public void case02_anonymousUserRedirectsToLoginFromStaffDetail() throws Exception {
        open("staff-detail?id=1");

        waitForLoginPage();

        assertTrue(driver.getCurrentUrl().contains("/login"));
        assertBodyContains("Login");
        assertBodyDoesNotContain("Admin User");
        capture("B1b_redirect_login_2_sau.png");
    }

    @Test
    public void case03_staffUserIsBlockedFromAdminCreateForm() throws Exception {
        login(driver, wait, STAFF_EMAIL, STAFF_PASSWORD);
        capture("B2_staff_bi_chan_403_1_truoc.png");

        clickLink("Add New Staff");

        assertBodyContains("403 - Forbidden");
        assertBodyContains("You do not have permission to access this page.");
        assertBodyContains("Back to List");
        assertBodyDoesNotContain("Add New Staff");
        capture("B2_staff_bi_chan_403_2_sau.png");
    }

    @Test
    public void case04_adminCanOpenCreateForm() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);
        capture("B3_admin_vao_form_1_truoc.png");

        clickLink("Add New Staff");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fullName")));
        assertTrue(driver.getCurrentUrl().contains("staff-crud?action=create"));
        assertBodyContains("Add New Staff");
        capture("B3_admin_vao_form_2_sau.png");
    }

    @Test
    public void case05_staffDetailShowsInformationWithoutPassword() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);
        clickViewForRowContaining("Admin User");
        capture("D1_chi_tiet_khong_password_1_truoc.png");

        assertBodyContains("Staff Detail");
        assertBodyContains("ID");
        assertBodyContains("Full Name");
        assertBodyContains("Gender");
        assertBodyContains("Phone Number");
        assertBodyContains("Email");
        assertBodyContains("Role");
        assertBodyContains("Status");
        assertBodyContains("Admin User");
        assertPageDoesNotExposePassword();
        capture("D1_chi_tiet_khong_password_2_sau.png");
    }

    @Test
    public void case06_backToListReturnsFromDetailPage() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);
        clickViewForRowContaining("Admin User");

        clickLink("Back to List");

        waitForStaffListPage();
        assertTrue(driver.getCurrentUrl().contains("/staff-list"));
    }

    @Test
    public void case07_nonNumericStaffIdShows400Page() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);

        open("staff-detail?id=abc");

        assertBodyContains("400 - Bad Request");
        assertBodyContains("Invalid staff ID.");
        capture("D3_id_chu_400_2_sau.png");
    }

    @Test
    public void case08_zeroStaffIdShows400Page() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);

        open("staff-detail?id=0");

        assertBodyContains("400 - Bad Request");
        assertBodyContains("Invalid staff ID.");
        capture("D4_id_khong_400_2_sau.png");
    }

    @Test
    public void case09_unknownStaffIdShows404Page() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);

        open("staff-detail?id=99999");

        assertBodyContains("404 - Staff Not Found");
        assertBodyContains("The requested staff does not exist.");
        capture("D5_id_khong_ton_tai_404_2_sau.png");
    }

    @Test
    public void case10_deletedStaffCannotBeViewedByDetailUrl() throws Exception {
        login(driver, wait, ADMIN_EMAIL, ADMIN_PASSWORD);
        String suffix = new SimpleDateFormat("MMddHHmmss").format(new Date());
        String fullName = "Tran Thi Mai " + suffix;
        String email = "maitt.selenium." + suffix + "@company.com";
        String phoneNumber = "09" + suffix.substring(suffix.length() - 8);
        int deletedStaffId = createStaff(driver, wait, fullName, phoneNumber,
                email, "mai12345", "Staff");

        WebElement deleteLink = findRowContaining(fullName).findElement(By.linkText("Delete"));
        capture("D6a_truoc_khi_xoa.png");
        deleteLink.click();
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        alert.accept();

        waitForStaffListPage();
        assertBodyDoesNotContain(fullName);
        capture("D6a_sau_khi_xoa.png");

        open("staff-detail?id=" + deletedStaffId);

        assertBodyContains("404 - Staff Not Found");
        assertBodyContains("The requested staff does not exist.");
        capture("D6b_da_xoa_404_2_sau.png");
    }

    private static void verifyApplicationResponds() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl).openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setRequestMethod("GET");
        int status = connection.getResponseCode();
        assertTrue("Application must respond before Selenium tests run: " + status,
                status >= 200 && status < 500);
    }

    private static void ensureStaffAccountExists(WebDriver setupDriver, WebDriverWait setupWait) {
        filterByKeyword(setupDriver, setupWait, STAFF_NAME);
        if (pageContains(setupDriver, STAFF_NAME)) {
            return;
        }
        createStaff(setupDriver, setupWait, STAFF_NAME, "0912345678",
                STAFF_EMAIL, STAFF_PASSWORD, "Staff");
    }

    private static int createStaff(WebDriver activeDriver, WebDriverWait activeWait,
            String fullName, String phoneNumber, String email, String password, String roleName) {
        clickLink(activeDriver, activeWait, "Add New Staff");
        activeWait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fullName")));
        activeDriver.findElement(By.id("fullName")).sendKeys(fullName);
        activeDriver.findElement(By.id("male")).click();
        activeDriver.findElement(By.id("phoneNumber")).sendKeys(phoneNumber);
        activeDriver.findElement(By.id("email")).sendKeys(email);
        activeDriver.findElement(By.id("password")).sendKeys(password);
        new Select(activeDriver.findElement(By.id("roleID"))).selectByVisibleText(roleName);
        activeDriver.findElement(By.id("active")).click();
        activeDriver.findElement(By.cssSelector("button[type='submit']")).click();
        waitForStaffListPage(activeDriver, activeWait);
        filterByKeyword(activeDriver, activeWait, fullName);
        WebElement row = findRowContaining(activeDriver, activeWait, fullName);
        return Integer.parseInt(row.findElement(By.xpath("./td[1]")).getText().trim());
    }

    private static void login(WebDriver activeDriver, WebDriverWait activeWait,
            String email, String password) {
        activeDriver.get(absoluteUrl("login"));
        activeWait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
        WebElement emailInput = activeDriver.findElement(By.id("email"));
        emailInput.clear();
        emailInput.sendKeys(email);
        WebElement passwordInput = activeDriver.findElement(By.id("password"));
        passwordInput.clear();
        passwordInput.sendKeys(password);
        activeDriver.findElement(By.cssSelector("button[type='submit']")).click();
        waitForStaffListPage(activeDriver, activeWait);
    }

    private static void filterByKeyword(WebDriver activeDriver, WebDriverWait activeWait,
            String keyword) {
        activeDriver.get(absoluteUrl("staff-list"));
        waitForStaffListPage(activeDriver, activeWait);
        WebElement keywordInput = activeDriver.findElement(By.name("searchKeyword"));
        keywordInput.clear();
        keywordInput.sendKeys(keyword);
        activeDriver.findElement(By.cssSelector("button[type='submit']")).click();
        waitForStaffListPage(activeDriver, activeWait);
    }

    private void open(String relativeUrl) {
        driver.get(absoluteUrl(relativeUrl));
    }

    private static String absoluteUrl(String relativeUrl) {
        String cleanRelativeUrl = relativeUrl;
        while (cleanRelativeUrl.startsWith("/")) {
            cleanRelativeUrl = cleanRelativeUrl.substring(1);
        }
        return baseUrl + cleanRelativeUrl;
    }

    private void clickLink(String text) {
        clickLink(driver, wait, text);
    }

    private static void clickLink(WebDriver activeDriver, WebDriverWait activeWait, String text) {
        activeWait.until(ExpectedConditions.elementToBeClickable(By.linkText(text))).click();
    }

    private void clickViewForRowContaining(String text) {
        findRowContaining(text).findElement(By.linkText("View")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("body")));
    }

    private WebElement findRowContaining(String text) {
        return findRowContaining(driver, wait, text);
    }

    private static WebElement findRowContaining(WebDriver activeDriver, WebDriverWait activeWait,
            String text) {
        By locator = By.xpath("//tr[td[contains(normalize-space(.), "
                + xpathLiteral(text) + ")]]");
        return activeWait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    private void waitForLoginPage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("email")));
    }

    private void waitForStaffListPage() {
        waitForStaffListPage(driver, wait);
    }

    private static void waitForStaffListPage(WebDriver activeDriver, WebDriverWait activeWait) {
        activeWait.until(ExpectedConditions.urlContains("/staff-list"));
        activeWait.until(ExpectedConditions.visibilityOfElementLocated(By.tagName("table")));
    }

    private void assertBodyContains(String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), text));
        assertTrue(bodyText().contains(text));
    }

    private void assertBodyDoesNotContain(String text) {
        assertFalse(bodyText().contains(text));
    }

    private void assertPageDoesNotExposePassword() {
        String body = bodyText().toLowerCase(Locale.ENGLISH);
        String source = driver.getPageSource().toLowerCase(Locale.ENGLISH);
        assertFalse(body.contains("password"));
        assertFalse(body.contains("hash"));
        assertFalse(source.contains("passwordhash"));
        assertFalse(source.contains("$2a$"));
        assertFalse(source.contains("$2b$"));
        assertFalse(source.contains("$2y$"));
    }

    private String bodyText() {
        return driver.findElement(By.tagName("body")).getText();
    }

    private static boolean pageContains(WebDriver activeDriver, String text) {
        return activeDriver.findElement(By.tagName("body")).getText().contains(text);
    }

    private void capture(String fileName) throws IOException {
        try {
            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(source.toPath(), evidenceDirectory.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (WebDriverException e) {
            throw new IOException("Could not capture Selenium evidence " + fileName, e);
        }
    }

    private static WebDriver createWebDriver() {
        String browser = configValue("selenium.browser", "SMS_SYSTEM_TEST_BROWSER", "chrome")
                .toLowerCase(Locale.ENGLISH);
        boolean headless = Boolean.parseBoolean(configValue("selenium.headless",
                "SMS_SYSTEM_TEST_HEADLESS", "true"));
        if ("edge".equals(browser)) {
            EdgeOptions options = new EdgeOptions();
            options.addArguments("--incognito");
            options.addArguments("--window-size=1365,900");
            if (headless) {
                options.addArguments("--headless=new");
            }
            return new EdgeDriver(options);
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--incognito");
        options.addArguments("--window-size=1365,900");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return new ChromeDriver(options);
    }

    private static String configValue(String propertyName, String environmentName,
            String defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue.trim();
        }
        return defaultValue;
    }

    private static String normalizeBaseUrl(String value) {
        return value.endsWith("/") ? value : value + "/";
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        throw new IllegalArgumentException("Unsupported XPath literal: " + value);
    }
}
