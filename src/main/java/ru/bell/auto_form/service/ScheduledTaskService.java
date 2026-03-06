package ru.bell.auto_form.service;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.model.dto.AnswerDTO;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ScheduledTaskService {
    private static final int DAYS_FOR_UPDATE_TOKEN = 10;

    @Setter
    private boolean state = false;

    private final TokenService tokenService;
    private final FormService formService;
    private final CurrentConfigProperties currentProperties;
    private final WorkService workService;
    private final EmailService emailService;

    public ScheduledTaskService(TokenService tokenService, FormService formService,
                                CurrentConfigProperties currentProperties, WorkService workService, EmailService emailService) {
        this.tokenService = tokenService;
        this.formService = formService;
        this.currentProperties = currentProperties;
        this.workService = workService;
        this.emailService = emailService;
    }

    @Scheduled(initialDelay = DAYS_FOR_UPDATE_TOKEN, fixedRate = DAYS_FOR_UPDATE_TOKEN, timeUnit = TimeUnit.DAYS)
    public void updateTokenEachTenDays() {
        try {
            log.debug("Token is update after ten days.");
            tokenService.checkToken();
            log.info("Token is update after ten days is successful.");
        } catch (Exception e) {
            log.error("Error in ScheduledTaskService: updateTokenEachTenDays().", e);
            emailService.sendExceptionMessage("Произошла ошибка в ScheduledTaskService: " + e.getMessage());
        }
    }

    @Scheduled(initialDelayString = "${current.polling_time_milliseconds}", fixedRateString = "${current.polling_time_milliseconds}")
    public void work() {
        try {
            while (!state) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread {} was interrupted.", Thread.currentThread().getName(), e);
                    emailService.sendExceptionMessage("Поток " + Thread.currentThread().getName() + " был прерван: " + e.getMessage());
                    return;
                }
            }

            int pollingIntervalMultiplier = 3;
            List<AnswerDTO> answers = formService.getAnswersInLastSeconds(pollingIntervalMultiplier * (currentProperties.getPollingTimeMilliseconds() / 1000));

            workService.work(answers);
        } catch (Exception e) {
            log.error("Error in ScheduledTaskService.", e);
            emailService.sendExceptionMessage("Произошла ошибка в ScheduledTaskService: " + e.getMessage());
        }
    }
}
