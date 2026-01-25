package ru.bell.auto_form.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.model.ResponseToken;
import ru.bell.auto_form.storage.TokenStorage;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TokenService {
    private final String URI_BASE = "https://oauth.yandex.ru";
    private final TokenStorage tokenStorage;
    private final RestTemplate restTemplate;
    private final YandexConfigProperties yandexConfigProperties;
    private final JsonService jsonService;

    @Autowired
    public TokenService(TokenStorage tokenStorage, RestTemplate restTemplate, YandexConfigProperties yandexConfigProperties, JsonService jsonService) {
        this.tokenStorage = tokenStorage;
        this.restTemplate = restTemplate;
        this.yandexConfigProperties = yandexConfigProperties;
        this.jsonService = jsonService;
    }

    public String getUriToCodeRequest() {
        String uriBase = URI_BASE + "/authorize";
        UriBuilder uriBuilder = UriComponentsBuilder.fromUriString(uriBase)
                .queryParam("response_type", "code")
                .queryParam("client_id", yandexConfigProperties.getClientId())
                .queryParam("redirect_uri", yandexConfigProperties.getRedirectUri());
        return uriBuilder.toUriString();
    }

    public void getTokenByCode(String code) {
        if (code != null && !code.isBlank()) {
            setToken("authorization_code", "code", code);
        } else {
            String error = "getTokenByCode(): code is null";
            log.error(error);
            throw new RuntimeException(error);
        }
    }

    public void updateToken() {
        if (tokenStorage != null && tokenStorage.getRefreshToken() != null)
            setToken("refresh_token", "refresh_token", tokenStorage.getRefreshToken());
        else {
            String error;
            if (tokenStorage == null)
                error = "updateToken(): tokenStorage is null.";
            else {
                error = "updateToken(): tokenStorage.getRefreshToken() is null.";
            }
            log.error(error);
            throw new RuntimeException(error);
        }
    }

    private void setToken(String grant_type, String param, String value) {
        String uriBase = URI_BASE + "/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Host", "https://oauth.yandex.ru/");
        headers.add("Content-type", "application/x-www-form-urlencoded");
        headers.add("Authorization", "Basic " + HttpHeaders.encodeBasicAuth(yandexConfigProperties.getClientId(), yandexConfigProperties.getSecretClientId(), StandardCharsets.UTF_8));

        MultiValueMap<String, String> requestBodyMap = new LinkedMultiValueMap<>();
        requestBodyMap.add("grant_type", grant_type);
        requestBodyMap.add(param, value);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(requestBodyMap, headers);
        log.debug("setToken(): uriBuilder: {}", uriBase);

        ResponseEntity<String> response = restTemplate.exchange(uriBase, HttpMethod.POST, entity, String.class);
        log.debug("setToken(): responseStatusCode: {}", response.getStatusCode());
        log.debug("setToken(): responseHeaders: {}", response.getHeaders());
        log.debug("setToken(): responseBody: {}", response.getBody());

        ResponseToken responseToken = jsonService.parseJsonToResponseToken(response.getBody());
        log.debug("setToken(): responseToken: {}", responseToken);

        tokenStorage.setToken(responseToken);
        log.debug("setToken(): tokenStorage: {}", tokenStorage);
    }
}
