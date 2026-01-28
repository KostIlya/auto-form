package ru.bell.auto_form.model.mapper;

import org.springframework.stereotype.Component;
import ru.bell.auto_form.model.ResponseToken;
import ru.bell.auto_form.model.YandexToken;

import java.time.LocalDateTime;

@Component
public class TokenMapper {
    private final YandexToken yandexToken;
    public TokenMapper(YandexToken yandexToken) {
        this.yandexToken = yandexToken;
    }
    public void fromResponseTokenToYandexToken(ResponseToken responseToken) {
        yandexToken.setAccessToken(responseToken.getAccessToken());
        yandexToken.setRefreshToken(responseToken.getRefreshToken());
        yandexToken.setExpiresAt(LocalDateTime.now().plusSeconds(responseToken.getExpiresIn()));
    }
}
