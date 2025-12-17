package ru.bell.auto_form.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;

@Service
public class FormService {


    @Value("${yandex.token}")
    private String token;

    private final RestTemplate restTemplate;

    public FormService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAll(String id) {
        final String baseUrl = "https://api.forms.yandex.net/v1/answers";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "OAuth " + token);
//        headers.set("Host", "api.forms.yandex.net");
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("answer_id", id);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    uriBuilder.toUriString(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            System.out.println(URLDecoder.decode(response.getBody()));

            return "ok";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
