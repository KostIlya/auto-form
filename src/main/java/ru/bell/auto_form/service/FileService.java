package ru.bell.auto_form.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class FileService {
    public void deleteFile(List<String> filePaths) {
        try {
            Thread.sleep(5000);

            for (var filePath : filePaths)
                Files.delete(Paths.get(filePath));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
