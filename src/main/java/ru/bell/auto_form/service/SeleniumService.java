package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.config.YandexTwoConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SeleniumService {
    @Autowired
    private final YandexTwoConfigProperties yandexTwoConfigProperties;

    public SeleniumService(YandexTwoConfigProperties yandexTwoConfigProperties) {
        this.yandexTwoConfigProperties = yandexTwoConfigProperties;
    }

    public void loginYandexDisk(WebDriver driver, WebDriverWait wait) {
        driver.get(yandexTwoConfigProperties.getUrlDisk());

        WebElement loginEl = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("react-aria-«R9ib»")));
        loginEl.clear();
        loginEl.sendKeys(yandexTwoConfigProperties.getLogin());

        driver.findElement(By.xpath("//button[.//span[text()='Далее']]")).click();

        WebElement passwordEl = wait.until(ExpectedConditions.presenceOfElementLocated(By
                .xpath("//input[@placeholder='Пароль']")));
        passwordEl.clear();
        passwordEl.sendKeys(yandexTwoConfigProperties.getPassword());

        driver.findElement(By.xpath("//button[.//span[text()='Далее']]")).click();

//        wait.until(ExpectedConditions.presenceOfElementLocated(By
//                .xpath("//button[.//span[text()='Напомнить позже']]")));
//        driver.findElement(By.xpath("//button[.//span[text()='Напомнить позже']]")).click();
        wait.until(ExpectedConditions.elementToBeClickable(By
                .xpath("//button[.//span[text()='Напомнить позже']]"))).click();
        try {
            Thread.sleep(7000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public WebElement getCell(WebDriver driver, WebDriverWait wait) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.className("volga-frame")));

        return wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//textarea[@data-testid='main-area-input']")));
    }

    public List<String> getIds(Actions actions, WebElement cellEl) {
        List<String> ids = new ArrayList<>();

        actions.keyDown(Keys.ARROW_DOWN).perform();

        while (!cellEl.getAttribute("value").isEmpty()) {
            ids.add(cellEl.getAttribute("value"));
            actions.keyDown(Keys.ARROW_DOWN).perform();
        }

        return ids;
    }

    public void addRow(AnswerDTO answerDTO, Actions actions, WebElement cellEl) {
        if (!cellEl.getAttribute("value").isEmpty()) {
            log.error("addRow(): row is not empty");
            throw new RuntimeException("addRow(): row is not empty");
        }
        int countFieldsAnswerDTO = 10;

        try {
            Thread.sleep(200);
            actionSendKeys(actions, answerDTO.getId());

            actionSendKeys(actions, answerDTO.getCreatedAt().toString());

            actionSendKeys(actions, answerDTO.getFio());

            actionSendKeys(actions, answerDTO.getAge().toString());

            actionSendKeys(actions, answerDTO.getLocation());

            actionSendKeys(actions, answerDTO.getTelegram());

            actionSendKeys(actions, answerDTO.getEmail());

            actionSendKeys(actions, answerDTO.getHr());

            actionSendKeys(actions, answerDTO.getResume());

            actions.sendKeys(answerDTO.getQuestionnaire()).perform();

            actionArrowRight(actions);

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

    private void actionSendKeys(Actions actions, String text) throws InterruptedException {
        actions.sendKeys(text).perform();
        actionArrowRight(actions);
    }

    private void actionArrowRight(Actions actions) throws InterruptedException {
        actions.keyDown(Keys.ARROW_RIGHT).perform();
        Thread.sleep(200);
    }
}
