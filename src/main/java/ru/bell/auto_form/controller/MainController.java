package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.service.DiskService;
import ru.bell.auto_form.service.FileService;
import ru.bell.auto_form.service.FormService;
import ru.bell.auto_form.service.XlsxService;

import java.io.*;
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
    private final YandexConfigProperties yandexProperties;
    @Autowired
    private final CurrentConfigProperties currentProperties;

    public MainController(CurrentConfigProperties currentProperties, YandexConfigProperties yandexProperties) {
        this.currentProperties = currentProperties;
        this.yandexProperties = yandexProperties;
    }

    @Scheduled(fixedDelayString = "${current.polling_time_milliseconds}")
    public void work() {
        // получаю данные с формы
        List<AnswerDTO> answers = formService.getAnswersInLastSeconds(currentProperties.getPollingTimeMilliseconds() / 1000);
        List<String> tempFilesPaths = new ArrayList<>();
        // скачиваю файлы из ответов
        try {
            for (AnswerDTO answer : answers) {
                String fileNameResume = uploadOneFileOnYandexDisk(answer.getResume(), tempFilesPaths);
                answer.setResume(diskService.publicFile(fileNameResume));

                String fileNameQuestionnaire = uploadOneFileOnYandexDisk(answer.getQuestionnaire(), tempFilesPaths);
                answer.setQuestionnaire(diskService.publicFile(fileNameQuestionnaire));
            }
            // заполняю таблицу

//        log.info("work(): answers: {}", answers);

            if (!answers.isEmpty())
                fillXlsxFile(answers, tempFilesPaths);
        } finally {
            fileService.deleteFile(tempFilesPaths);
        }
        log.info("FROM MainController!");
    }

    private String uploadOneFileOnYandexDisk(String href, List<String> tempFilesPath) {
        try {
            String filePath = diskService.downloadFileFromYandexFormForLink(href);
            // загружаю файлы на яндекс диск
            log.debug("work path to file before upload: {}", filePath);
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

    private void fillXlsxFile(List<AnswerDTO> answers, List<String> tempFilesPath) {
        String fileName = "/answers.xlsx";
        try {
            String filePath = diskService.download(fileName);
            xlsxService.appendAnswers(answers, filePath);
            tempFilesPath.add(filePath);
            try (InputStream is = new FileInputStream(filePath)) {
                diskService.upload(is, fileName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}


//            String fileNameResume = "/foldir/6948ff731f1eb564d86a8fc8_rezyumetestivan.docx";