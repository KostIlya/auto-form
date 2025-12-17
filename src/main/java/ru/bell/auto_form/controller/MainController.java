package ru.bell.auto_form.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import ru.bell.auto_form.service.DiskService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Controller
@Slf4j
public class MainController {
    @Autowired
    private DiskService diskService;

    //@Scheduled(fixedDelay = 600000)
    public void work() {
        // получаю данные с формы

        // скачиваю файлы из ответа
        try {
            String href = "https://forms.yandex.ru/u/files?path=%2F4412411%2F693be033e010db2b8c79729f_oprosniktest.docx";
            String pathFile = diskService.downloadForLink(href);
            // загружаю файлы на яндекс диск
            log.debug("work path to file before upload: {}",pathFile);
            String filename = pathFile.substring(pathFile.lastIndexOf('\\') + 1);

            InputStream is = new FileInputStream(pathFile);
            String fullFileName = "/foldir/" + filename;
            String hrefFile = diskService.upload(is, fullFileName);

            // заполняю таблицу

        } catch (IOException e) {
            log.error(e.getMessage());
        }

        log.debug("FROM MainController!");
    }
}
