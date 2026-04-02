package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.WebDriverFactory;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.config.YandexTwoConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeleniumService {
    private final YandexTwoConfigProperties yandexTwoConfigProperties;
    private final XlsxService xlsxService;
    private final YandexConfigProperties yandexConfigProperties;
    private final CurrentConfigProperties currentProperties;
    public SeleniumService(YandexTwoConfigProperties yandexTwoConfigProperties, XlsxService xlsxService,
                           YandexConfigProperties yandexConfigProperties, CurrentConfigProperties currentProperties) {
        this.yandexTwoConfigProperties = yandexTwoConfigProperties;
        this.xlsxService = xlsxService;
        this.yandexConfigProperties = yandexConfigProperties;
        this.currentProperties = currentProperties;
    }

    public WebElement getCell(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.className("volga-frame")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='spreadsheet-editor']")));

        WebElement inputElement = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[@data-testid='main-area-input']")
        ));

        log.debug("getCell(): Found input element \"cell\"");

        return inputElement;
    }

    public List<String> getIds(Actions actions, WebElement cellEl) {
        List<String> ids = new ArrayList<>();

        actions.keyDown(Keys.ARROW_DOWN).perform();
        String text = cellEl.findElement(By.tagName("span")).getAttribute("innerHTML");

        while (text != null && !text.isEmpty()) {
            ids.add(text);
            actions.keyDown(Keys.ARROW_DOWN).perform();

            try {
                text = cellEl.findElement(By.tagName("span")).getAttribute("innerHTML");
            } catch (Exception e) {
                text = null;
            }
        }
        return ids;
    }

    public void addRow(AnswerDTO answerDTO, Actions actions, WebElement cellEl, WebDriver driver) throws RuntimeException {
        String text = null;
        try {
            text = cellEl.findElement(By.tagName("span")).getAttribute("innerHTML");
        } catch (Exception e) { }
        if (text != null && !text.isEmpty()) {
            log.error("addRow(): row is not empty");
            throw new RuntimeException("addRow(): row is not empty");
        }
        int countFieldsAnswerDTO = 10;

        try {
            Thread.sleep(200);
            actionSendKeys(actions, answerDTO.getId(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getCreatedAt().toString(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getFio(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getAge().toString(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getLocation(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getTelegram(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getEmail(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getHr(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getResume(), driver, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getQuestionnaire(), driver, answerDTO.getId());

            actions.keyDown(Keys.ARROW_DOWN).perform();
            for (int i = 0; i < countFieldsAnswerDTO; i++) {
                actions.keyDown(Keys.ARROW_LEFT).perform();
                Thread.sleep(200);
            }
        } catch (Exception e) {
            log.error("addRow(): {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void actionSendKeys(Actions actions, String text, WebDriver driver, String answerId) throws InterruptedException, RuntimeException {
        checkCaptchaByAddRow(driver, answerId);
        actions.sendKeys(text).perform();
        actionArrowRight(actions);
    }

    private void actionArrowRight(Actions actions) throws InterruptedException {
        actions.keyDown(Keys.ARROW_RIGHT).perform();
        Thread.sleep(200);
    }

    private void checkCaptchaByAddRow(WebDriver webDriver, String answerId) throws RuntimeException {
        boolean isRobot = false;
        try {
            if (webDriver.getTitle().equals("Вы не робот?")) {
                isRobot = true;
            } else {
                webDriver.findElement(By.xpath("//input[@class='CheckboxCaptcha-Button']"));
                isRobot = true;
            }
        } catch (Exception e) {
            log.debug("checkCaptchaByAddRow(): Captcha is not found:" + e.getMessage());
        }
        if (isRobot) {
            throw new RuntimeException("Возникла captcha при добавлении записи с id = " + answerId);
        }
    }

    public Integer execute() {
        Integer count = 0;
        WebDriver webDriver = WebDriverFactory.createDriver();
        WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(yandexTwoConfigProperties.getUrlDisk());
        webDriverWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete';"));
        Actions actions = new Actions(webDriver);

        WebElement cell = getCell(webDriver, webDriverWait);
        List<String> idsFromAnswers2 = getIds(actions, cell);
        String tableAnswersName = yandexConfigProperties.getTableAnswersName().startsWith("/") ?
                yandexConfigProperties.getTableAnswersName().substring(1)
                : yandexConfigProperties.getTableAnswersName();
        String filePathExcel = currentProperties.getTmpDir() + tableAnswersName;
        try (FileInputStream file = new FileInputStream(filePathExcel);
             Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            int countRows = sheet.getLastRowNum() + 1;
            for (int i = 1; i < countRows; i++) {
                if (!idsFromAnswers2.contains(xlsxService.getId(sheet, i))) {
                    AnswerDTO answerDTO = xlsxService.getAnswerDTO(sheet, i);
                    addRow(answerDTO, actions, cell, webDriver);
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("executeSelenium(): FileInputStream(filePathExcel): {}", e.getMessage());
            throw new RuntimeException(e);
        }

        WebDriverFactory.closeDriver(webDriver);
        return count;
    }
}
