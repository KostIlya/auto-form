package ru.bell.auto_form.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class RestClientService {
    private final RestTemplate restTemplate;

    public RestClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    ///
    @Retryable(retryFor = {ResourceAccessException.class},
        maxAttempts = 5,
        backoff = @Backoff(delay = 3000))
    public <T, R> ResponseEntity<R> exchangeTwoParam(RequestEntity<T> requestEntity, Class<R> responseType) {
        return restTemplate.exchange(requestEntity, responseType);
    }

    @Retryable(retryFor = {ResourceAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 3000))
    public <T, R>ResponseEntity<R> exchangeFourParam(String url, HttpMethod method, HttpEntity<T> httpEntity, Class<R> responseType) {
        return restTemplate.exchange(
                url,
                method,
                httpEntity,
                responseType
        );
    }
}
