package ru.bell.auto_form.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FormService {


    @Value("${yandex.token}")
    private String token;

    private final RestTemplate restTemplate;

    public FormService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getAll() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Host", "api.forms.yandex.net");

        return "ok";
    }

}
