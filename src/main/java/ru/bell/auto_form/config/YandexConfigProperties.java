package ru.bell.auto_form.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@AllArgsConstructor
@ConfigurationProperties(prefix="yandex")
public class YandexConfigProperties {
    private final String token;
}
