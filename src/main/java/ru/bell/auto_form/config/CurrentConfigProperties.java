package ru.bell.auto_form.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "current")
@Setter
@Getter
public class CurrentConfigProperties {
    private String downloadDir;
    private Integer pollingTimeMilliseconds;
    private String separator = File.separator;

    public String getDownloadDir() {
        if (!downloadDir.endsWith(separator))
            return downloadDir + separator;
        return downloadDir;
    }
}
