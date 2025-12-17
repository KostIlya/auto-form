package ru.bell.auto_form.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "current")
@Setter
public class CurrentConfigProperties {
    private String downloadDir;

    public String getDownloadDir() {
        if (!downloadDir.endsWith(File.separator))
            return downloadDir + File.separator;
        return downloadDir;
    }
}
