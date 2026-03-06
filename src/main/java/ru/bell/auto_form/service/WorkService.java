package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Service
@Slf4j
public class WorkService {
    private final XlsxService xlsxService;
    private final DiskService diskService;
    private final SeleniumService seleniumService;
    private final YandexConfigProperties yandexConfigProperties;
    private final FileService fileService;
    private final EmailService emailService;

    public WorkService(XlsxService xlsxService, DiskService diskService, SeleniumService seleniumService, YandexConfigProperties yandexConfigProperties, FileService fileService, EmailService emailService) {
        this.xlsxService = xlsxService;
        this.diskService = diskService;
        this.seleniumService = seleniumService;
        this.yandexConfigProperties = yandexConfigProperties;
        this.fileService = fileService;
        this.emailService = emailService;
    }

    public void work(List<AnswerDTO> answers) {
        List<String> tempFilesPaths = new ArrayList<>();
        try {
            log.info("Run...");

            Integer countIntermediateAnswersRecordings = 0;
            String fileName = yandexConfigProperties.getTableAnswersName().startsWith("/") ?
                    yandexConfigProperties.getTableAnswersName() : "/" + yandexConfigProperties.getTableAnswersName().trim();
            String filePath = diskService.download(fileName);
            tempFilesPaths.add(filePath);
            Set<String> existAnswersIds = xlsxService.getAllExistsAnswersIds(filePath);
            // скачиваю файлы из ответов
            for (AnswerDTO answer : answers) {
                if (!xlsxService.isExistAnswer(answer.getId(), existAnswersIds)) {
                    String fileNameResume = uploadOneFileOnYandexDisk(answer.getResume(), tempFilesPaths);
                    answer.setResume(diskService.publicFile(fileNameResume));

                    String fileNameQuestionnaire = uploadOneFileOnYandexDisk(answer.getQuestionnaire(), tempFilesPaths);
                    answer.setQuestionnaire(diskService.publicFile(fileNameQuestionnaire));
                }
            }
            // заполняю таблицу
            if (!answers.isEmpty()) {
                countIntermediateAnswersRecordings = fillXlsxFile(answers, filePath, fileName);
            }
            printResult("IntermediateTable", countIntermediateAnswersRecordings);

            Integer countAnswersRecordings = seleniumService.execute();
            printResult("ResultTable", countAnswersRecordings);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
