package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bell.auto_form.model.ResponseToken;
import ru.bell.auto_form.model.YandexToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class FileService {
    public void createIfNotExistFile(String path) {
        try {
            if (!isExist(path))
                Files.createFile(Paths.get(path));
        } catch (IOException e) {
            log.error("createFile: file {} has not been create", path);
            throw new RuntimeException(e);
        }
    }

    public void fillCsvFileWithYandexToken(String path, YandexToken yandexToken, String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(yandexToken.getAccessToken());
        sb.append(separator);
        sb.append(yandexToken.getRefreshToken());
        sb.append(separator);
        sb.append(yandexToken.getExpiresAt());
        log.debug("fillCsvFileWithYandexToken(): {}", sb.toString());
        try {
            Files.writeString(Paths.get(path), sb.toString(), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readYandexTokenFromCsvFile(String path, YandexToken yandexToken, String separator) {
        try {
            String[] words = Files.readString(Paths.get(path)).split(separator);
            log.debug("readYandexTokenFromCsvFile(): {}", Files.readString(Paths.get(path)));
            yandexToken.setAccessToken(words[0]);
            yandexToken.setRefreshToken(words[1]);
            yandexToken.setExpiresAt(LocalDateTime.parse(words[2]));
            log.debug("readYandexTokenFromCsvFile(): at = {}, rt = {}, ea = {}.", yandexToken.getAccessToken(),
                    yandexToken.getRefreshToken(), yandexToken.getExpiresAt());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isExist(String path) {
        return Files.exists(Paths.get(path));
    }

    public boolean isValidCsvWithYandexToken(String path, String separator) {
        String line;
        try {
            line = Files.readString(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String[] words = line.split(separator);
        if (line.isBlank()) {
            return false;
        } else if (words.length != 3) {
            return false;
        } else {
            for (var w : words) {
                if (w.isBlank() || w.equals("null")) {
                    return false;
                }
            }
        }

        return true;
    }

    public void deleteFiles(List<String> filePaths) {
        try {
            Thread.sleep(5000);

            for (var filePath : filePaths)
                Files.delete(Paths.get(filePath));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
