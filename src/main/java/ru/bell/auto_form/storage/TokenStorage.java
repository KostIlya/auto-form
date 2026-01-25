package ru.bell.auto_form.storage;

import lombok.*;
import org.springframework.stereotype.Component;
import ru.bell.auto_form.model.ResponseToken;

import java.time.LocalDateTime;

@Component
@Getter
@ToString
public class TokenStorage {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;

    public void setToken(ResponseToken responseToken) {
        accessToken = responseToken.getAccessToken();
        refreshToken = responseToken.getRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(responseToken.getExpiresIn());
    }
}
