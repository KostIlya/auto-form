package ru.bell.auto_form.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class AppService {
    @Autowired
    private ApplicationContext context;

    public void exitWithCode(Integer code) {
        int exitCode = SpringApplication.exit(context, () -> code);
        System.exit(exitCode);
    }
}
