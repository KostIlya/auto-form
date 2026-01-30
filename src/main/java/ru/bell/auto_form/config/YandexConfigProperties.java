package ru.bell.auto_form.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix="yandex")
public class YandexConfigProperties {
    private String clientId;
    private String secretClientId;
    private String redirectUri;
    private String filesDirectory;
    private String tableAnswersName = "/answers.xlsx";
    private String surveyId;
    private String csvTokenPath = "tokenStorage.csv";
    private String csvSeparator = ";";
}
