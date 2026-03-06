package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    @Value("${spring.mail.to}")
    private String to;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }


    public void sendExceptionMessage(String message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject("В приложении AutoForm произошла ошибка.");
        msg.setText(message);

        log.debug("sendExceptionMessage(): {}", mailSender.toString());
        try {
            mailSender.send(msg);
        }
        catch (MailException e) {
            log.error("sendExceptionMessage(): {}", e.getMessage());
        }
    }
}
