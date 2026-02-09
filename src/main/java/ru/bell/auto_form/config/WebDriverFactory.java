package ru.bell.auto_form.config;

//import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebDriverFactory {
    public static WebDriver createDriver() {
        try {
//            WebDriverManager.chromedriver().setup();
            System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
            options.setBinary("/usr/local/bin/google-chrome");
            return new ChromeDriver(options);
        } catch (Exception e) {
            log.error("Error created WebDriver", e);
            throw new RuntimeException("Error created WebDriver", e);
        }
    }

    public static void closeDriver(WebDriver driver) {
        if (driver != null) {
            try {
                driver.quit();
                log.debug("WebDriver closed");
            } catch (Exception e) {
                log.error("Error closed WebDriver", e);
            }
        }
    }
}
