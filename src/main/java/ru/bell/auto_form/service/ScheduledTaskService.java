package ru.bell.auto_form.service;


import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.config.YandexTwoConfigProperties;
import ru.bell.auto_form.exception.CriticalException;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScheduledTaskService {
    private static final int DAYS_FOR_UPDATE_TOKEN = 10;

    @Setter
    private boolean state = false;

    private final TokenService tokenService;
    private final DiskService diskService;
    private final FormService formService;
    private final XlsxService xlsxService;
    private final FileService fileService;
    private final SeleniumService seleniumService;
    private final YandexConfigProperties yandexConfigProperties;
    private final CurrentConfigProperties currentProperties;
    private final AppService appService;

    public ScheduledTaskService(TokenService tokenService, DiskService diskService, FormService formService,
                                XlsxService xlsxService, FileService fileService, SeleniumService seleniumService,
                                CurrentConfigProperties currentProperties, YandexConfigProperties yandexConfigProperties,
                                AppService appService) {
        this.tokenService = tokenService;
        this.diskService = diskService;
        this.formService = formService;
        this.xlsxService = xlsxService;
        this.fileService = fileService;
        this.seleniumService = seleniumService;
        this.currentProperties = currentProperties;
        this.yandexConfigProperties = yandexConfigProperties;
        this.appService = appService;
    }

    @Scheduled(initialDelay = DAYS_FOR_UPDATE_TOKEN, fixedRate = DAYS_FOR_UPDATE_TOKEN, timeUnit = TimeUnit.DAYS)
    public void updateTokenEachTenDays() {
        log.debug("Token is update after ten days.");
        tokenService.checkToken();
        log.info("Token is update after ten days is successful.");
    }

    @Scheduled(fixedRateString = "${current.polling_time_milliseconds}")
    public void work() {
        while (!state) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> tempFilesPaths = new ArrayList<>();
        try {
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

            printResult("IntermediateTable", countIntermediateAnswersRecordings);
            Integer countAnswersRecordings = seleniumService.execute();

            printResult("ResultTable", countAnswersRecordings);
        } catch (CriticalException e) {
            log.error("work(): ", e);
            appService.exitWithCode(1);
        } catch (Exception e) {
            log.error("work(): {}", e.toString());
        } finally {
            fileService.deleteFiles(tempFilesPaths);
        }
        log.info("Finish.");
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

    private void printResult(String nameTable, Integer countAnswersRecordings) {
        if (countAnswersRecordings > 0)
            log.info("{}: New answers to the form have been recorded. There were {} answers recorded.",
                    nameTable, countAnswersRecordings);
        else {
            log.info("{}: New answers is not.", nameTable);
        }
    }
}
