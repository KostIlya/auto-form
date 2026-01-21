package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.WebDriverFactory;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.config.YandexTwoConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.service.*;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
public class MainController {
    @Autowired
    private DiskService diskService;
    @Autowired
    private FormService formService;
    @Autowired
    private XlsxService xlsxService;
    @Autowired
    private FileService fileService;
    @Autowired
    private SeleniumService seleniumService;
    @Autowired
    private final YandexConfigProperties yandexProperties;
    @Autowired
    private final CurrentConfigProperties currentProperties;

    @Autowired
    private YandexTwoConfigProperties yandexTwoConfigProperties;
    public MainController(CurrentConfigProperties currentProperties, YandexConfigProperties yandexProperties) {
        this.currentProperties = currentProperties;
        this.yandexProperties = yandexProperties;
    }

    @Scheduled(fixedDelayString = "${current.polling_time_milliseconds}")
    public void work() {
        // получаю данные с формы
        List<AnswerDTO> answers = formService.getAnswersInLastSeconds(currentProperties.getPollingTimeMilliseconds() / 1000);
        List<String> tempFilesPaths = new ArrayList<>();
        Integer countAnswersRecordings = 0;
        // скачиваю файлы из ответов
        try {
            for (AnswerDTO answer : answers) {
                String fileNameResume = uploadOneFileOnYandexDisk(answer.getResume(), tempFilesPaths);
                answer.setResume(diskService.publicFile(fileNameResume));

                String fileNameQuestionnaire = uploadOneFileOnYandexDisk(answer.getQuestionnaire(), tempFilesPaths);
                answer.setQuestionnaire(diskService.publicFile(fileNameQuestionnaire));
            }
            // заполняю таблицу

            String fileName = yandexProperties.getTableAnswersName().startsWith("/") ? yandexProperties.getTableAnswersName() : "/" + yandexProperties.getTableAnswersName().trim();
            String filePath = diskService.download(fileName);
            tempFilesPaths.add(filePath);
            if (!answers.isEmpty())
                countAnswersRecordings = fillXlsxFile(answers, filePath, fileName);

            executeSelenium();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            fileService.deleteFile(tempFilesPaths);
        }
        if (countAnswersRecordings > 0)
            log.info("New answers to the form have been recorded. There were {} answers recorded.", countAnswersRecordings);
        else {
            log.info("New answers is not.");
        }
    }

    private String uploadOneFileOnYandexDisk(String href, List<String> tempFilesPath) {
        try {
            String filePath = diskService.downloadFileFromYandexFormForLink(href);
            // загружаю файлы на яндекс диск
            String filename = filePath.substring(filePath.lastIndexOf('\\') + 1);

            String fullFileName = yandexProperties.getFilesDirectory() + "/" + filename;
            try (InputStream is = new FileInputStream(filePath)) {
                diskService.upload(is, fullFileName);
            }
            tempFilesPath.add(filePath);
            return fullFileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer fillXlsxFile(List<AnswerDTO> answers, String filePath, String fileName) {

        Integer countAnswersRecordings = 0;
        try {
            countAnswersRecordings = xlsxService.appendAnswers(answers, filePath);
            try (InputStream is = new FileInputStream(filePath)) {
                diskService.upload(is, fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return countAnswersRecordings;
    }

    private void executeSelenium() {
        WebDriver webDriver = WebDriverFactory.createDriver();
        WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(yandexTwoConfigProperties.getUrlDisk());
//        seleniumService.loginYandexDisk(webDriver, webDriverWait);
        Actions actions = new Actions(webDriver);

        WebElement cell = seleniumService.getCell(webDriver, webDriverWait);
        List<String> idsFromAnswers2 = seleniumService.getIds(actions, cell);
        String filePathExcel = currentProperties.getDownloadDir() + yandexProperties.getTableAnswersName();
        try (FileInputStream file = new FileInputStream(filePathExcel);
             Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            int countRows = sheet.getLastRowNum() + 1;
            for (int i = 1; i < countRows; i++) {
                if (!idsFromAnswers2.contains(xlsxService.getId(sheet, i))) {
                    AnswerDTO answerDTO = xlsxService.getAnswerDTO(sheet, i);
                    seleniumService.addRow(answerDTO, actions, cell);
                }
            }
        } catch (Exception e) {
            log.error("work(): FileInputStream(filePathExcel): {}", e.getMessage());
            throw new RuntimeException(e);
        }

        WebDriverFactory.closeDriver(webDriver);
    }
}