package ru.bell.auto_form.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "current")
@Setter
@Getter
@Slf4j
public class CurrentConfigProperties {
    private String tmpDir;
    private Integer pollingTimeMilliseconds;
    private String separator = File.separator;

    @PostConstruct
    public void init() {
        createTmpDir();
    }

    public void createTmpDir() {
        Path path = Paths.get("tmp");

        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                log.error("setTmpDir(): fail create tmp directory.");
                throw new RuntimeException(e);
            }
        }
        tmpDir = path.toAbsolutePath() + separator;
        log.debug("getTmpDir(): AbsolutePath: {}", tmpDir);
    }
}
