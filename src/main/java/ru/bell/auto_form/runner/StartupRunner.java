package ru.bell.auto_form.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;
import ru.bell.auto_form.service.*;

import java.nio.file.Paths;
import java.util.List;

@Component
@Slf4j
public class StartupRunner implements CommandLineRunner {
    private final YandexConfigProperties yandexConfigProperties;
    private final CurrentConfigProperties currentConfigProperties;
    private final TokenService tokenService;
    private final FileService fileService;
    private final FormService formService;
    private final DiskService yandexDiskService;
    private final ScheduledTaskService scheduledTaskService;
    private final WorkService workService;

    public StartupRunner(YandexConfigProperties yandexConfigProperties, CurrentConfigProperties currentConfigProperties,
                         TokenService tokenService, FileService fileService, FormService formService, DiskService yandexDiskService,
                         ScheduledTaskService scheduledTaskService, WorkService workService) {
        this.yandexConfigProperties = yandexConfigProperties;
        this.currentConfigProperties = currentConfigProperties;
        this.tokenService = tokenService;
        this.fileService = fileService;
        this.formService = formService;
        this.yandexDiskService = yandexDiskService;
        this.scheduledTaskService = scheduledTaskService;
        this.workService = workService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("run(): create if not exist csv file with Token");
        fileService.createIfNotExistFile(yandexConfigProperties.getCsvTokenPath());
        log.debug("run(): check token");
        tokenService.checkTokenOnStartup();
        log.debug("run(): create (if not exist) a folder to store resumes and questionnaires.");
        yandexDiskService.createDirectory(yandexConfigProperties.getFilesDirectory());
        String tableAnswers1Name = yandexConfigProperties.getTableAnswersName().startsWith("/") ?
                yandexConfigProperties.getTableAnswersName().substring(1) : yandexConfigProperties.getTableAnswersName().trim();
        String tableAnswers1Path = Paths.get(currentConfigProperties.getTemplateAnswersTable1Path()).toAbsolutePath()
                + currentConfigProperties.getSeparator() + tableAnswers1Name;
        log.debug("run(): create (if not exist) a file {} on yandex.disk 1.", tableAnswers1Name);
        yandexDiskService.createFile(tableAnswers1Path);

        // получаю данные с формы
        List<AnswerDTO> answers = formService.getAllAnswers();
        workService.work(answers);

        scheduledTaskService.setState(true);
        log.debug("run(): StartupRunner finish");
    }
}
