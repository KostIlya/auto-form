package ru.bell.auto_form.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


@Service
public class DiskService {

    @Value("${yandex.token}")
    private String token;

    private final RestTemplate restTemplate;

    public DiskService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
            throw new IOException("Error during getting link");
        }
        String href = response.getBody().href();
        System.out.println(href);

        RequestEntity<Void> requestDownload = RequestEntity.get(href)
                .build();

        byte[] responseDownload = restTemplate.getForObject(href, byte[].class);
//        if (!responseDownload.getStatusCode().is2xxSuccessful()) {
//            throw new IOException("Error downloading file");
//        }
        String projectRoot = System.getProperty("user.dir");
        String resourcesPath = projectRoot + "/src/main/resources/files/";

        String filename = Arrays.stream(href.split("&"))
                .filter((s) -> s.startsWith("filename="))
                .map(s -> s.split("=")[1])
                .findFirst().orElseThrow(() -> new RuntimeException("Error parse href for filename"));

        try (FileOutputStream fos = new FileOutputStream(resourcesPath + filename)) {
            fos.write(responseDownload);
        } catch (Exception e) {
            System.out.println("Folder not found: " + e.getMessage());
        }

        return "File is downloaded successfully to folder /resources/files/" + filename;
    }











    public String downloadForLink(String href) throws IOException {

        String uri = href.split("\\?",2)[0];
        System.out.println(uri);

//        RequestEntity<Void> requestDownload = RequestEntity.get(UriComponentsBuilder
//                        .fromUriString(uri)
//                        .path(href.split("\\?",2)[1].split("=")[1])
//                        .build()
//                        .toUri()
//                ).header("Authorization", "OAuth " + token)
//                .build();
//
//        ResponseEntity<byte[]> responseDownload = restTemplate.exchange(requestDownload, byte[].class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> responseDownload = restTemplate.exchange(
                href,
                HttpMethod.GET,
                entity,
                byte[].class
        );
        if (!responseDownload.getStatusCode().is2xxSuccessful()) {
            throw new IOException("Error downloading file");
        }
        String contentDisposition = responseDownload.getHeaders().getFirst("Content-Disposition");
        System.out.println("contentDisposition = " + contentDisposition);
        String projectRoot = System.getProperty("user.dir");
        String resourcesPath = projectRoot + "/src/main/resources/files/";


//        String filename = Arrays.stream(href.split("&"))
//                .filter((s) -> s.startsWith("filename="))
//                .map(s -> s.split("=")[1])
//                .findFirst().orElseThrow(() -> new RuntimeException("Error parse href for filename"));

        String[] p = href.split("\\?",2)[1].split("=")[1].split("%2F");
        String filename = p[p.length - 1];
        System.out.println(filename);
        try (FileOutputStream fos = new FileOutputStream(resourcesPath + filename)) {
            fos.write(responseDownload.getBody());
        } catch (Exception e) {
            System.out.println("Folder not found: " + e.getMessage());
        }

        return "File is downloaded successfully to folder /resources/files/" + filename;
    }














    public void upload(InputStream is, String fullFileName) throws IOException {
        final String baseUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload";

        RequestEntity<Void> requestEntity = RequestEntity.get(
                        UriComponentsBuilder.fromUriString(baseUrl)
                                .queryParam("path", fullFileName)
                                .build()
                                .toUri()
                ).header("Authorization", "OAuth " + token)
                .build();

        ResponseEntity<Link> linkResponseEntity = restTemplate.exchange(requestEntity, Link.class);

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
            System.out.println("File is uploaded successfully");
        } else {
            System.out.println(responseToUpload.getBody() + " | " + responseToUpload.getStatusCode());
        }
    }

    public void createDirectory(String path) {
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
                System.out.println("Successfully created folder: " + path);
            }
        } catch (Exception e) {
            System.out.println("Folder already exists: " + e.getMessage());
        }
    }

    public void setToken(String token) {
        this.token = token;
    }
}

record Link(String href, String method, boolean templated) {
}
