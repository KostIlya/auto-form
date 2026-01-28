package ru.bell.auto_form.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Getter
@Setter
@Component
public class YandexToken {
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
}
