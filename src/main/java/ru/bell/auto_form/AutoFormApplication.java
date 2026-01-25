package ru.bell.auto_form;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.service.DiskService;

@SpringBootApplication
public class AutoFormApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AutoFormApplication.class, args);
        YandexConfigProperties yandexConfigProperties = context.getBean(YandexConfigProperties.class);
        DiskService yandexDiskService = context.getBean(DiskService.class);
//        yandexDiskService.createDirectory(yandexConfigProperties.getFilesDirectory());
    }
}

