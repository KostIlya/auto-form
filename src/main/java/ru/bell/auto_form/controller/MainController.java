package ru.bell.auto_form.controller;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.WebDriverFactory;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.config.YandexTwoConfigProperties;
import ru.bell.auto_form.exception.CriticalException;
import ru.bell.auto_form.model.YandexToken;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.service.*;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
public class MainController {
    @Autowired
    private TokenService tokenService;
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
    private final YandexConfigProperties yandexConfigProperties;
    @Autowired
    private final CurrentConfigProperties currentProperties;
    @Autowired
    private YandexToken yandexToken;
    @Autowired
    private ApplicationContext context;

    @Autowired
    private YandexTwoConfigProperties yandexTwoConfigProperties;

    public MainController(CurrentConfigProperties currentProperties, YandexConfigProperties yandexConfigProperties) {
        this.currentProperties = currentProperties;
        this.yandexConfigProperties = yandexConfigProperties;
    }

    @Scheduled(fixedRateString = "${current.polling_time_milliseconds}")
    public void work() {
        List<String> tempFilesPaths = new ArrayList<>();
        try {
            checkToken();

            log.info("Run...");
            // получаю данные с формы
            List<AnswerDTO> answers = formService.getAnswersInLastSeconds(currentProperties.getPollingTimeMilliseconds() / 1000);

            Integer countIntermediateAnswersRecordings = 0;
            // скачиваю файлы из ответов
            for (AnswerDTO answer : answers) {
                String fileNameResume = uploadOneFileOnYandexDisk(answer.getResume(), tempFilesPaths);
                answer.setResume(diskService.publicFile(fileNameResume));

                String fileNameQuestionnaire = uploadOneFileOnYandexDisk(answer.getQuestionnaire(), tempFilesPaths);
                answer.setQuestionnaire(diskService.publicFile(fileNameQuestionnaire));
            }
            // заполняю таблицу

            String fileName = yandexConfigProperties.getTableAnswersName().startsWith("/") ?
                    yandexConfigProperties.getTableAnswersName() : "/" + yandexConfigProperties.getTableAnswersName().trim();
            String filePath = diskService.download(fileName);
            tempFilesPaths.add(filePath);

            if (!answers.isEmpty()) {
                countIntermediateAnswersRecordings = fillXlsxFile(answers, filePath, fileName);
            }

            Integer countAnswersRecordings = executeSelenium();

            printResult("IntermediateTable", countIntermediateAnswersRecordings);
            printResult("ResultTable", countAnswersRecordings);
        } catch (CriticalException e) {
            log.error("work(): ", e);
            int exitCode = SpringApplication.exit(context, () -> 1);
            System.exit(exitCode);
        } catch (Exception e) {
            log.error("work(): {}", e.toString());
        } finally {
            fileService.deleteFiles(tempFilesPaths);
        }
        log.info("Finish.");
    }

    private void checkToken() {
        log.debug("checkToken(): run.");
        String path = yandexConfigProperties.getCsvTokenPath();
        String separator = yandexConfigProperties.getCsvSeparator();
        if (fileService.isExist(path) && fileService.isValidCsvWithYandexToken(path, separator)) {
            fileService.readYandexTokenFromCsvFile(path, yandexToken, separator);

            if (LocalDateTime.now().isAfter(yandexToken.getExpiresAt().minusMonths(5))) {
                log.debug("checkToken(): try update token.");

                try {
                    tokenService.updateToken();
                } catch (CriticalException e) {
                    log.error("checkToken(): ", e);
                    int exitCode = SpringApplication.exit(context, () -> 1);
                    System.exit(exitCode);
                }
            }
        } else {
            log.info("Please, go to the endpoint /auth/start to authorize the application in the OAuth.Yandex service.");
            log.info("Wait...");
            while (yandexToken == null || yandexToken.getAccessToken() == null) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error("checkToken(): {}", e.getMessage());
                }
            }
        }

        log.debug("checkToken(): finish.");
    }

    private String uploadOneFileOnYandexDisk(String href, List<String> tempFilesPath) {
        try {
            String filePath = diskService.downloadFileFromYandexFormForLink(href);
            // загружаю файлы на яндекс диск
            String filename = new File(filePath).getName();

            String fullFileName = yandexConfigProperties.getFilesDirectory() + "/" + filename;
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

    private Integer executeSelenium() {
        Integer count = 0;
        WebDriver webDriver = WebDriverFactory.createDriver();
        WebDriverWait webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(30));
        webDriver.get(yandexTwoConfigProperties.getUrlDisk());
        webDriverWait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete';"));
//        seleniumService.loginYandexDisk(webDriver, webDriverWait);
        Actions actions = new Actions(webDriver);

        WebElement cell = seleniumService.getCell(webDriver, webDriverWait);
        List<String> idsFromAnswers2 = seleniumService.getIds(actions, cell);
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
                    seleniumService.addRow(answerDTO, actions, cell);
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

    private void printResult(String nameTable, Integer countAnswersRecordings) {
        if (countAnswersRecordings > 0)
            log.info("{}: New answers to the form have been recorded. There were {} answers recorded.",
                    nameTable, countAnswersRecordings);
        else {
            log.info("{}: New answers is not.", nameTable);
        }
    }
}