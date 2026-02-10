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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import ru.bell.auto_form.config.YandexConfigProperties;
import ru.bell.auto_form.exception.CriticalException;
import ru.bell.auto_form.model.ResponseToken;
import ru.bell.auto_form.model.YandexToken;
import ru.bell.auto_form.model.mapper.TokenMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
@Slf4j
public class TokenService {
    private final String URI_BASE = "https://oauth.yandex.ru";
    private final YandexToken yandexToken;
    private final RestClientService restClientService;
    private final YandexConfigProperties yandexConfigProperties;
    private final JsonService jsonService;
    private final FileService fileService;
    private final TokenMapper tokenMapper;
    private final AppService appService;

    @Autowired
    public TokenService(YandexToken yandexToken, RestClientService restClientService, YandexConfigProperties yandexConfigProperties,
                        JsonService jsonService, FileService fileService, TokenMapper tokenMapper, AppService appService) {
        this.yandexToken = yandexToken;
        this.restClientService = restClientService;
        this.yandexConfigProperties = yandexConfigProperties;
        this.jsonService = jsonService;
        this.fileService = fileService;
        this.tokenMapper = tokenMapper;
        this.appService = appService;
    }

    public String getUriToCodeRequest() {
        String uriBase = URI_BASE + "/authorize";
        UriBuilder uriBuilder = UriComponentsBuilder.fromUriString(uriBase)
                .queryParam("response_type", "code")
                .queryParam("client_id", yandexConfigProperties.getClientId())
                .queryParam("redirect_uri", yandexConfigProperties.getRedirectUri());
        return uriBuilder.toUriString();
    }

    public void getTokenByCode(String code) throws CriticalException {
        log.debug("getTokenByCode()");
        if (code != null && !code.isBlank()) {
            setToken("authorization_code", "code", code);
        } else {
            String error = "getTokenByCode(): code is null";
            log.error(error);
            throw new CriticalException(error);
        }
    }

    public void updateToken() throws CriticalException {
        log.debug("updateToken()");
        if (yandexToken != null && yandexToken.getRefreshToken() != null)
            setToken("refresh_token", "refresh_token", yandexToken.getRefreshToken());
        else {
            String error;
            if (yandexToken == null)
                error = "updateToken(): tokenStorage is null.";
            else {
                error = "updateToken(): tokenStorage.getRefreshToken() is null.";
            }
            log.error(error);
            throw new CriticalException(error);
        }
    }

    private void setToken(String grant_type, String param, String value) throws CriticalException {
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
        try {
            ResponseEntity<String> response = restClientService.exchangeFourParam(uriBase, HttpMethod.POST, entity, String.class);
            log.debug("setToken(): responseStatusCode: {}", response.getStatusCode());
            log.debug("setToken(): responseHeaders: {}", response.getHeaders());
            log.debug("setToken(): responseBody: {}", response.getBody());

            ResponseToken responseToken = jsonService.parseJsonToResponseToken(response.getBody());
            log.debug("setToken(): responseToken: {}", responseToken);

            tokenMapper.fromResponseTokenToYandexToken(responseToken);

            fileService.fillCsvFileWithYandexToken(yandexConfigProperties.getCsvTokenPath(), yandexToken,
                    yandexConfigProperties.getCsvSeparator());

            log.debug("setToken(): yandexToken: {}", yandexToken.getAccessToken());
        } catch (HttpClientErrorException e) {
            log.error("setToken(): status: {}, body: {}", e.getStatusText(), e.getResponseBodyAsString());
            throw new CriticalException(e);
        } catch (RestClientException e) {
            log.error("setToken(): {}", e.getMessage());
            throw new CriticalException(e);
        }
    }

    public void checkToken() {
        log.debug("checkToken(): run.");
        String path = yandexConfigProperties.getCsvTokenPath();
        String separator = yandexConfigProperties.getCsvSeparator();
        if (fileService.isExist(path) && fileService.isValidCsvWithYandexToken(path, separator)
                && !LocalDateTime.now().isAfter(yandexToken.getExpiresAt())) {
            if (LocalDateTime.now().isAfter(yandexToken.getExpiresAt().minusMonths(5))) {
                log.debug("checkToken(): try update token.");

                try {
                    updateToken();
                } catch (CriticalException e) {
                    log.error("checkToken(): ", e);
                    appService.exitWithCode(1);
                }
            }
        } else {
            waitCompleteAuthStart();
        }

        log.debug("checkToken(): finish.");
    }

    public void checkTokenOnStartup() {
        log.debug("checkTokenOnStartup(): run.");

        String path = yandexConfigProperties.getCsvTokenPath();
        String separator = yandexConfigProperties.getCsvSeparator();
        if (fileService.isExist(path) && fileService.isValidCsvWithYandexToken(path, separator)) {
            fileService.readYandexTokenFromCsvFile(path, yandexToken, separator);

            if (LocalDateTime.now().isBefore(yandexToken.getExpiresAt())) {
                log.debug("checkTokenOnStartup(): try update token.");
                try {
                    updateToken();
                } catch (CriticalException e) {
                    log.error("checkTokenOnStartup(): ", e);
                    appService.exitWithCode(1);
                }
            } else {
                log.debug("checkTokenOnStartup(): the token has expired");
                waitCompleteAuthStart();
            }
        } else {
            log.debug("checkTokenOnStartup(): token doesn't exist yet.");
            waitCompleteAuthStart();
        }
        log.debug("checkTokenOnStartup(): finish.");

    }

    public void waitCompleteAuthStart() {
        log.info("Please, go to the endpoint /auth/start to authorize the application in the OAuth.Yandex service.");
        log.info("Wait...");
        while (yandexToken == null || yandexToken.getAccessToken() == null) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("waitCompleteAuthStart(): {}", e.getMessage());
            }
        }
    }
}
