package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.bell.auto_form.config.CurrentConfigProperties;
import ru.bell.auto_form.model.PublishFileResponse;
import ru.bell.auto_form.model.record.Link;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class DiskService {

    @Value("${yandex.token}")
    private String token;
    @Autowired
    private CurrentConfigProperties currentProperties;
    private final RestTemplate restTemplate;

    public DiskService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Создание директории на яндекс диске
    public void createDirectory(String path) {
        List<String> dirs = getDirs(path);

        for (int i = 0; i < dirs.size(); i++) {
            String currentDirectory = getCurrentDir(dirs, i + 1);
            if (!isExistDirectory(currentDirectory)) {
                createDirectory(currentDirectory, true);
            }
        }
    }

    // Получение из пути path списка директорий
    private List<String> getDirs(String path) {
        List<String> dirs = Arrays.stream(path.split("/")).filter(s -> !s.isBlank()).toList();
        log.info("dirs: {}", dirs);
        return dirs;
    }

    // Формирование пути текущей директории
    private String getCurrentDir(List<String> dirs, Integer numberDirectory) {
        StringBuilder res = new StringBuilder(numberDirectory);
        for (int i = 0; i < numberDirectory; i++) {
            res.append("/");
            res.append(dirs.get(i));
        }
        return res.toString();
    }

    private boolean isExistDirectory(String path) {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources";
        log.info("isExistDIR: {}", path);
        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build()
                                .toUri()
                ).header("Authorization", "OAuth " + token)
                .build();
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(requestEntity, String.class);

            return response.getStatusCode().equals(HttpStatusCode.valueOf(200));
        } catch (RestClientException e) {
            log.error(e.getMessage());
            return false;
        }
    }

    // Создание конкретной директории
    private void createDirectory(String path, Boolean b) {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources";

        RequestEntity<Void> requestEntity = RequestEntity.put(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build().toUri()
                )
                .header("Authorization", "OAuth " + token)
                .build();
        try {
            ResponseEntity<String> exchange = restTemplate.exchange(requestEntity, String.class);
            HttpStatusCode statusCode = exchange.getStatusCode();
            if (statusCode.equals(HttpStatusCode.valueOf(201))) {
                log.info("Successfully created folder: {}", path);
            }
        } catch (Exception e) {
            log.error("Path {}: {}", path, e.getMessage());
        }
    }

    // Загрузка на яндекс диск
    // fullFileName - путь от корня яндекс диска до загружаемого файла(/dir1/file1.docx)
    public void upload(InputStream is, String fullFileName) throws IOException {
        if (is == null || fullFileName == null) {
            throw new NullPointerException("is or fullFileName is null");
        }

        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload";

        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", fullFileName)
                                .queryParam("overwrite", "true")
                                .build()
                                .toUri()
                ).header("Authorization", "OAuth " + token)
                .build();

        ResponseEntity<Link> linkResponseEntity = restTemplate.exchange(requestEntity, Link.class);

        if (!linkResponseEntity.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            throw new RuntimeException("Href wasn't get. HttpStatus: " + linkResponseEntity.getStatusCode() +
                    ". Body: " + linkResponseEntity.getBody());
        }

        String link = linkResponseEntity.getBody().href();

        RequestEntity<byte[]> requestToUpload = RequestEntity.put(
                UriComponentsBuilder.fromUriString(link)
                        .build()
                        .toUri()
        ).body(is.readAllBytes());

        ResponseEntity<String> responseToUpload = restTemplate.exchange(
                requestToUpload, String.class
        );

        if (responseToUpload.getStatusCode().is2xxSuccessful()) {
            log.info("File is uploaded successfully");
        } else {
            throw new RuntimeException("File wasn't upload. HttpStatus: " + responseToUpload.getStatusCode() +
                    ". Body: " + responseToUpload.getBody());
        }
    }

    // Скачать файл по ссылке
    public String downloadFileFromYandexFormForLink(String href) throws IOException {
        String baseUrl = "https://forms.yandex.ru/u/files";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth " + token);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String linkk = URLDecoder.decode(href);
        ResponseEntity<byte[]> response = restTemplate.exchange(
                linkk,
                HttpMethod.GET,
                entity,
                byte[].class
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Error downloading file");
        }
//        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
//        log.warn("JSON STATUS CODE response download: {}", response.getStatusCode());
//        log.warn("JSON HEADERS response download: {}", response.getHeaders());
//        log.warn("JSON BODY response download: {}", response.getBody());
//        //
//        if (contentDisposition != null)
//            log.info("contentDisposition = {}", URLDecoder.decode(contentDisposition));
//        else
//            log.error("contentDisposition = null");
        String[] p = href.split("\\?", 2)[1].split("=")[1].split("/");
        String filename = p[p.length - 1];
        String destination = currentProperties.getDownloadDir() + filename;

        try (FileOutputStream fos = new FileOutputStream(destination)) {
            fos.write(response.getBody());
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        log.info("File is downloaded successfully to path: {}", destination);
        return destination;
    }

    // Скачать с яндекс диска
    public String download(String path) throws IOException {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/download";
        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", path)
                                .build().toUri()
                )
                .header("Authorization", "OAuth " + token)
                .build();

        ResponseEntity<Link> response = restTemplate.exchange(requestEntity, Link.class);
        if (!response.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            throw new RuntimeException("Error during getting link");
        }
        String href = response.getBody().href();
        System.out.println(href);

        String linkkk = URLDecoder.decode(href);
        ResponseEntity<byte[]> responseDownload = restTemplate.exchange(
                linkkk,
                HttpMethod.GET,
                null,
                byte[].class
        );

        String projectRoot = System.getProperty("user.dir");
        String resourcesPath = projectRoot + "/src/main/resources/files/download/";

        String filename = Arrays.stream(href.split("&"))
                .filter((s) -> s.startsWith("filename="))
                .map(s -> s.split("=")[1])
                .findFirst().orElseThrow(() -> new RuntimeException("Error parse href for filename"));
        String filePath = currentProperties.getDownloadDir() + filename;
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(responseDownload.getBody());
        } catch (Exception e) {
            System.out.println("Folder not found: " + e.getMessage());
        }

        return filePath;
    }

    public String publicFile(String filePath) {
//        String line;
        String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/publish";
        RequestEntity<Void> request = RequestEntity.put(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", filePath)
                                .build().toUri()
                )
                .header("Authorization", "OAuth " + token)
                .header("Content-Type", "application/json")
                .build();

        log.info("publicFile(): request.getUrl(): {}", request.getUrl());
        ResponseEntity<Link> response = restTemplate.exchange(request, Link.class);
        log.info("publicFile: status: {}", response.getStatusCode());
        log.info("publicFile: Link {}", response.getBody());
        if (response.getStatusCode().equals(HttpStatusCode.valueOf(200))) {
            log.info("publicFile(): response.getBody().href(): {}", response.getBody().href());
            String linkPublishFile = URLDecoder.decode(response.getBody().href());
        log.info("publicFile(): linkPublishFile: {}", linkPublishFile);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<PublishFileResponse> responseEntity = restTemplate.exchange(
                    linkPublishFile,
                    HttpMethod.GET,
                    entity,
                    PublishFileResponse.class
            );

            if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(200)))
                return responseEntity.getBody().getPublic_url();
            else
                throw new RuntimeException("publicFile(): response.getStatusCode()=" + response.getStatusCode());
        } else {
            throw new RuntimeException("publicFile(): response.getStatusCode()=" + response.getStatusCode());
        }

    }
}
