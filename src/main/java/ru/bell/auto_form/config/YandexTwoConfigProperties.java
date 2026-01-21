package ru.bell.auto_form.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix="yandex-two")
public class YandexTwoConfigProperties {
    private String urlDisk;
    private String login;
    private String password;
}
