package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.service.DiskService;
import ru.bell.auto_form.service.FormService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Controller
@Slf4j
public class MainController {
    @Autowired
    private DiskService diskService;
    @Autowired
    private FormService formService;
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
        // скачиваю файлы из ответов
        for (AnswerDTO answer : answers) {
            String fileNameResume = uploadOneFileOnYandexDisk(answer.getResume());
            answer.setResume(getHrefToFileOnYandexDisk("fileNameResume"));

            String fileNameQuestionnaire = uploadOneFileOnYandexDisk(answer.getQuestionnaire());
            answer.setResume(getHrefToFileOnYandexDisk("fileNameQuestionnaire"));
        }
        // заполняю таблицу

        System.out.println(answers);

        if (!answers.isEmpty())
            fillCsvFile(answers);

        log.info("FROM MainController!");
    }

    private String uploadOneFileOnYandexDisk(String href) {
        try {
            String pathFile = diskService.downloadFileFromYandexFormForLink(href);
            // загружаю файлы на яндекс диск
            log.debug("work path to file before upload: {}", pathFile);
            String filename = pathFile.substring(pathFile.lastIndexOf('\\') + 1);

            InputStream is = new FileInputStream(pathFile);
            String fullFileName = yandexProperties.getFilesDirectory() + "/" + filename;
            diskService.upload(is, fullFileName);
            return fullFileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHrefToFileOnYandexDisk(String filePath) {

        return "https://disk.yandex.ru/i/27eHgh__VTemEQ";
    }

    private void fillCsvFile(List<AnswerDTO> answers) {
        String fileName = "/answers.csv";
        try {
            String filePath = diskService.download(fileName);
            for (AnswerDTO answer : answers) {
                Files.writeString(Path.of(filePath), answer.toString() + "\n", StandardOpenOption.APPEND);
            }
            InputStream is = new FileInputStream(filePath);

            diskService.upload(is, fileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
